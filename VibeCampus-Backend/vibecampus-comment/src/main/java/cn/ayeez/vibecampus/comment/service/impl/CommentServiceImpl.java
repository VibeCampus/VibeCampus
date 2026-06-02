package cn.ayeez.vibecampus.comment.service.impl;

import cn.ayeez.vibecampus.comment.dto.CommentPageResponse;
import cn.ayeez.vibecampus.comment.dto.CommentReplyResponse;
import cn.ayeez.vibecampus.comment.dto.CommentResponse;
import cn.ayeez.vibecampus.common.dto.AuthorInfo;
import cn.ayeez.vibecampus.common.dto.interaction.LikeToggleResponse;
import cn.ayeez.vibecampus.comment.mapper.CommentMapper;
import cn.ayeez.vibecampus.comment.mapper.CommentPostMapper;
import cn.ayeez.vibecampus.comment.model.CommentEntity;
import cn.ayeez.vibecampus.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论业务服务实现类。
 * <p>实现评论的增删改查、点赞、回复等核心功能，并同步更新帖子的评论计数。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final CommentPostMapper commentPostMapper;

    @Override
    public CommentPageResponse getCommentsByPostId(Long postId, Integer page,
                                                   Integer pageSize, Long currentUserId) {
        // 参数校验与默认值处理
        if (page == null || page <= 0) {
            page = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每页条数不能超过100");
        }

        // 计算分页偏移量
        int offset = (page - 1) * pageSize;

        // 查询一级评论列表和总数
        List<CommentEntity> comments = commentMapper.selectTopLevelComments(postId, offset, pageSize);
        long total = commentMapper.countTopLevelComments(postId);

        // 转换为响应DTO，并填充作者信息、点赞状态、二级回复
        List<CommentResponse> responseList = comments.stream()
                .map(comment -> buildCommentResponse(comment, currentUserId))
                .collect(Collectors.toList());

        return new CommentPageResponse(responseList, total, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentResponse createComment(Long postId, String content, Long currentUserId) {
        // 内容校验
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论内容不能为空");
        }
        if (content.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论内容不能超过1000字");
        }

        // 创建评论实体
        CommentEntity comment = new CommentEntity();
        comment.setPostId(postId);
        comment.setParentId(null); // 一级评论
        comment.setAuthorId(currentUserId);
        comment.setReplyToUserId(null);
        comment.setContent(content.trim());
        comment.setStatus(0); // 正常状态

        // 插入数据库
        commentMapper.insertComment(comment);

        // 同步更新帖子的评论计数
        commentPostMapper.incrementCommentCount(postId);

        // 返回完整评论信息
        return buildCommentResponse(comment, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentReplyResponse createReply(Long commentId, String content, Long replyToUserId, Long currentUserId) {
        // 内容校验
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能为空");
        }
        if (content.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能超过1000字");
        }

        // 验证父评论是否存在且未删除
        CommentEntity parentComment = commentMapper.selectById(commentId);
        if (parentComment == null || parentComment.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在或已被删除");
        }

        // 创建回复实体
        CommentEntity reply = new CommentEntity();
        reply.setPostId(parentComment.getPostId());
        reply.setParentId(commentId); // 二级回复
        reply.setAuthorId(currentUserId);
        reply.setReplyToUserId(replyToUserId);
        reply.setContent(content.trim());
        reply.setStatus(0);

        // 插入数据库
        commentMapper.insertComment(reply);

        // 同步更新帖子的评论计数
        commentPostMapper.incrementCommentCount(parentComment.getPostId());

        // 返回完整回复信息
        return buildReplyResponse(reply, currentUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LikeToggleResponse toggleCommentLike(Long commentId, Long currentUserId) {
        // 验证评论是否存在
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在或已被删除");
        }

        // 检查当前用户是否已点赞
        int existingLike = commentMapper.countCommentLike(currentUserId, commentId);
        boolean currentlyLiked = existingLike > 0;

        if (currentlyLiked) {
            // 取消点赞：删除点赞记录，减少点赞数
            commentMapper.deleteCommentLike(currentUserId, commentId);
            commentMapper.decrementLikeCount(commentId);
        } else {
            // 点赞：插入点赞记录，增加点赞数
            commentMapper.insertCommentLike(currentUserId, commentId);
            commentMapper.incrementLikeCount(commentId);
        }

        // 查询最新点赞数
        CommentEntity updatedComment = commentMapper.selectById(commentId);
        return new LikeToggleResponse(!currentlyLiked, updatedComment.getLikeCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long currentUserId) {
        // 查询评论（包含已删除的，用于判断是否存在）
        CommentEntity comment = commentMapper.selectByIdIncludeDeleted(commentId);
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在");
        }
        if (comment.getDeletedAt() != null) {
            throw new ResponseStatusException(HttpStatus.GONE, "评论已被删除");
        }

        // 权限校验：仅允许作者删除
        if (!comment.getAuthorId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能删除自己的评论");
        }

        // 软删除评论
        int result = commentMapper.softDeleteComment(commentId, currentUserId);
        if (result == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除评论失败");
        }

        // 同步更新帖子的评论计数
        commentPostMapper.decrementCommentCount(comment.getPostId());
    }

    /**
     * 构建一级评论响应对象。
     *
     * @param comment       评论实体
     * @param currentUserId 当前用户ID（用于判断点赞状态）
     * @return 评论响应DTO
     */
    private CommentResponse buildCommentResponse(CommentEntity comment, Long currentUserId) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setAuthorId(comment.getAuthorId());
        response.setContent(comment.getContent());
        response.setTime(formatTime(comment.getCreatedAt()));
        response.setLikes(comment.getLikeCount() == null ? 0 : comment.getLikeCount());

        // 填充作者信息
        AuthorInfo author = commentPostMapper.selectAuthorById(comment.getAuthorId());
        if (author != null) {
            response.setAuthor(author);
        }

        // 填充点赞状态
        if (currentUserId != null) {
            int likeCount = commentMapper.countCommentLike(currentUserId, comment.getId());
            response.setLiked(likeCount > 0);
        } else {
            response.setLiked(false);
        }

        // 查询二级回复
        List<CommentEntity> replies = commentMapper.selectRepliesByParentId(comment.getId());
        List<CommentReplyResponse> replyResponses = replies.stream()
                .map(reply -> buildReplyResponse(reply, currentUserId))
                .collect(Collectors.toList());
        response.setReplies(replyResponses);

        return response;
    }

    /**
     * 构建二级回复响应对象。
     *
     * @param reply         回复实体
     * @param currentUserId 当前用户ID（用于判断点赞状态）
     * @return 回复响应DTO
     */
    private CommentReplyResponse buildReplyResponse(CommentEntity reply, Long currentUserId) {
        CommentReplyResponse response = new CommentReplyResponse();
        response.setId(reply.getId());
        response.setParentId(reply.getParentId());
        response.setAuthorId(reply.getAuthorId());
        response.setReplyToUserId(reply.getReplyToUserId());
        response.setContent(reply.getContent());
        response.setTime(formatTime(reply.getCreatedAt()));
        response.setLikes(reply.getLikeCount() == null ? 0 : reply.getLikeCount());

        // 填充作者信息
        AuthorInfo author = commentPostMapper.selectAuthorById(reply.getAuthorId());
        if (author != null) {
            response.setAuthor(author);
        }

        // 填充被回复人信息
        if (reply.getReplyToUserId() != null) {
            AuthorInfo replyToUser = commentPostMapper.selectAuthorById(reply.getReplyToUserId());
            if (replyToUser != null) {
                response.setReplyToUser(replyToUser);
            }
        }

        // 填充点赞状态
        if (currentUserId != null) {
            int likeCount = commentMapper.countCommentLike(currentUserId, reply.getId());
            response.setLiked(likeCount > 0);
        } else {
            response.setLiked(false);
        }

        return response;
    }

    /**
     * 格式化时间为相对时间字符串。
     *
     * @param dateTime 日期时间
     * @return 相对时间字符串（如"5分钟前"、"2小时前"、"3天前"）
     */
    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return seconds + "秒前";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟前";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时前";
        } else if (seconds < 2592000) {
            return (seconds / 86400) + "天前";
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }
}
