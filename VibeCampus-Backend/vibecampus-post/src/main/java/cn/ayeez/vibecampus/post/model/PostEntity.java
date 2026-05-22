package cn.ayeez.vibecampus.post.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子实体：映射 posts 表核心字段，仅用于发帖场景。
 */
@Data
public class PostEntity {

    private Long id;

    private Long authorId;

    private String categorySlug;

    private String content;

    private Integer postType;

    private Integer status;

    private Integer anonymous;

    private Integer likeCount;

    private Integer commentCount;

    private Integer favoriteCount;

    private LocalDateTime createdAt;
}
