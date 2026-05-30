package cn.ayeez.vibecampus.comment.controller;

import cn.ayeez.vibecampus.comment.dto.CommentPageResponse;
import cn.ayeez.vibecampus.comment.dto.CommentReplyResponse;
import cn.ayeez.vibecampus.comment.dto.CommentResponse;
import cn.ayeez.vibecampus.common.dto.interaction.LikeToggleResponse;
import cn.ayeez.vibecampus.comment.service.CommentService;
import cn.ayeez.vibecampus.user.util.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评论接口控制器。
 * <p>提供评论的查询、发布、回复、点赞和删除等功能。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 获取帖子的评论列表（分页）。
     * <p>返回一级评论及其嵌套的二级回复，支持当前用户点赞状态填充。</p>
     *
     * @param postId   帖子ID
     * @param page     页码（从1开始，默认1）
     * @param pageSize 每页条数（默认20，最大100）
     * @return 评论分页响应，包含评论列表和总数
     */
    @GetMapping("/post/{postId}/comments")
    public CommentPageResponse getComments(
            @PathVariable Long postId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {

        log.info("查询评论列表，postId={}, page={}, pageSize={}", postId, page, pageSize);
        Long currentUserId = CurrentUserContext.getCurrentUserIdOptional();
        CommentPageResponse response = commentService.getCommentsByPostId(postId, page, pageSize, currentUserId);
        log.info("查询评论列表成功，postId={}, total={}", postId, response.getTotal());
        return response;
    }

    /**
     * 发布一级评论。
     * <p>为指定帖子创建顶级评论，自动同步更新帖子的评论计数。</p>
     *
     * @param postId 帖子ID
     * @param body   请求体，包含 content 字段（评论内容）
     * @return 创建的评论信息
     */
    @PostMapping("/posts/{postId}/comments")
    public CommentResponse createComment(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {

        Long currentUserId = CurrentUserContext.getCurrentUserId();
        String content = body.get("content");
        log.info("发布一级评论，postId={}, userId={}, contentLength={}", 
                postId, currentUserId, content == null ? 0 : content.length());
        CommentResponse response = commentService.createComment(postId, content, currentUserId);
        log.info("发布一级评论成功，commentId={}", response.getId());
        return response;
    }

    /**
     * 发布二级回复。
     * <p>对指定评论进行回复，形成嵌套结构，自动同步更新帖子的评论计数。</p>
     *
     * @param commentId 父评论ID
     * @param body      请求体，包含 content（回复内容）和 replyToUserId（被回复人ID，可选）
     * @return 创建的回复信息
     */
    @PostMapping("/comments/{commentId}/replies")
    public CommentReplyResponse createReply(
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> body) {

        Long currentUserId = CurrentUserContext.getCurrentUserId();
        String content = (String) body.get("content");
        Long replyToUserId = body.get("replyToUserId") != null ?
                ((Number) body.get("replyToUserId")).longValue() : null;
        log.info("发布二级回复，parentCommentId={}, userId={}, replyToUserId={}", 
                commentId, currentUserId, replyToUserId);
        CommentReplyResponse response = commentService.createReply(commentId, content, replyToUserId, currentUserId);
        log.info("发布二级回复成功，replyId={}", response.getId());
        return response;
    }

    /**
     * 切换评论点赞状态。
     * <p>幂等操作：首次点赞增加计数，再次点击取消点赞并减少计数。</p>
     *
     * @param commentId 评论ID
     * @return 点赞切换响应，包含当前点赞状态和总点赞数
     */
    @PostMapping("/comments/{commentId}/like")
    public LikeToggleResponse toggleCommentLike(
            @PathVariable Long commentId) {

        Long currentUserId = CurrentUserContext.getCurrentUserId();
        log.info("切换评论点赞，commentId={}, userId={}", commentId, currentUserId);
        LikeToggleResponse response = commentService.toggleCommentLike(commentId, currentUserId);
        log.info("切换评论点赞成功，commentId={}, liked={}, likeCount={}", 
                commentId, response.getLiked(), response.getLikeCount());
        return response;
    }

    /**
     * 删除评论（软删除）。
     * <p>仅允许评论作者删除自己的评论，删除后自动同步更新帖子的评论计数。</p>
     *
     * @param commentId 评论ID
     * @return 200 OK（空响应体）
     * @throws org.springframework.web.server.ResponseStatusException 403（非作者）、404（不存在）、410（已删除）
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId) {

        Long currentUserId = CurrentUserContext.getCurrentUserId();
        log.info("删除评论，commentId={}, userId={}", commentId, currentUserId);
        commentService.deleteComment(commentId, currentUserId);
        log.info("删除评论成功，commentId={}", commentId);
        return ResponseEntity.ok().build();
    }
}
