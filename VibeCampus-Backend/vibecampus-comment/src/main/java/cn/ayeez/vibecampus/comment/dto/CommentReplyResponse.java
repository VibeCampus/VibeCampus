package cn.ayeez.vibecampus.comment.dto;

import cn.ayeez.vibecampus.common.dto.AuthorInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二级回复响应DTO：用于返回评论的嵌套回复。
 * <p>包含回复基本信息、作者信息、被回复人信息以及点赞状态。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReplyResponse {

    /** 回复ID */
    private Long id;

    /** 父评论ID（一级评论的ID） */
    private Long parentId;

    /** 回复作者ID */
    private Long authorId;

    /** 作者摘要信息（用户名、头像等） */
    private AuthorInfo author;

    /** 被回复人ID */
    private Long replyToUserId;

    /** 被回复人摘要信息（用户名、头像等） */
    private AuthorInfo replyToUser;

    /** 回复内容 */
    private String content;

    /** 相对时间字符串（如"5分钟前"、"2小时前"） */
    private String time;

    /** 点赞数 */
    private Integer likes;

    /** 当前用户是否已点赞（未登录时为false） */
    private Boolean liked;

}
