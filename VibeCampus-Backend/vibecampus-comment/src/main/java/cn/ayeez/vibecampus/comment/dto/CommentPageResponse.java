package cn.ayeez.vibecampus.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论分页响应DTO：用于返回帖子的评论列表及分页信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageResponse {

    /** 评论列表（一级评论及其嵌套回复） */
    private List<CommentResponse> list;

    /** 评论总数 */
    private Long total;

    /** 当前页码（从1开始） */
    private Integer page;

    /** 每页条数 */
    private Integer pageSize;

}
