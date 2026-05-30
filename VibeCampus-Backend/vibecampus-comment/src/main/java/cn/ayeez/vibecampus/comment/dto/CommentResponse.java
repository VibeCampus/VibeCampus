package cn.ayeez.vibecampus.comment.dto;

import cn.ayeez.vibecampus.common.dto.AuthorInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 一级评论响应DTO：用于返回帖子的顶级评论列表。
 * <p>包含评论基本信息、作者信息、点赞状态以及嵌套的回复列表。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    /** 评论ID */
    private Long id;

    /** 所属帖子ID */
    private Long postId;

    /** 评论作者ID */
    private Long authorId;

    /** 作者摘要信息（用户名、头像等） */
    private AuthorInfo author;

    /** 评论回复的用户ID（一级评论时为null） */
    private Long replyToUserId;

    /** 回复的用户摘要信息（用户名、头像等，一级评论时为null） */
    private AuthorInfo replyToUser;

    /** 评论内容 */
    private String content;

    /** 相对时间字符串（如"5分钟前"、"2小时前"） */
    private String time;

    /** 点赞数 */
    private Integer likes;

    /** 当前用户是否已点赞（未登录时为false） */
    private Boolean liked;

    /** 二级回复列表（嵌套的回复） */
    private List<CommentReplyResponse> replies;

}
