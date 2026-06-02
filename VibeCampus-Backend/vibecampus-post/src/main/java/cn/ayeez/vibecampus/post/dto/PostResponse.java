package cn.ayeez.vibecampus.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发帖成功后的响应体，与 API 文档 9.3 的 Post 结构对齐。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;

    private String category;

    private String content;

    private Boolean anonymous;

    private Long authorId;

    /**
     * 作者摘要信息：用于帖子详情页展示，匿名帖子时为 null。
     */
    private PostAuthorResponse author;

    private List<String> images;

    private String time;

    private Integer likes;

    private Integer comments;

    /**
     * 收藏数
     */
    private Integer favorites;

    /**
     * 当前用户是否已点赞（未登录时为false）
     */
    private Boolean liked;

    /**
     * 当前用户是否已收藏（未登录时为false）
     */
    private Boolean favorited;
}
