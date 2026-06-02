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

    /**
     * 热度分数（用于热门帖子排序）
     */
    private Long hotScore;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     * <p>非null表示该帖子已被删除，查询时需过滤 deleted_at is null</p>
     */
    private LocalDateTime deletedAt;
}
