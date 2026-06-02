package cn.ayeez.vibecampus.comment.service;

import cn.ayeez.vibecampus.comment.dto.CommentPageResponse;
import cn.ayeez.vibecampus.comment.dto.CommentReplyResponse;
import cn.ayeez.vibecampus.comment.dto.CommentResponse;
import cn.ayeez.vibecampus.common.dto.interaction.LikeToggleResponse;

public interface CommentService {

    CommentPageResponse getCommentsByPostId(Long postId, Integer page, Integer pageSize, Long currentUserId);

    CommentResponse createComment(Long postId, String content, Long currentUserId);

    CommentReplyResponse createReply(Long commentId, String content, Long replyToUserId, Long currentUserId);

    LikeToggleResponse toggleCommentLike(Long commentId, Long currentUserId);

    void deleteComment(Long commentId, Long currentUserId);
}
