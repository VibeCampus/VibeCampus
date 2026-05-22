package cn.ayeez.vibecampus.post.model;

import lombok.Data;

/**
 * 帖子媒体实体：映射 post_media 表。
 */
@Data
public class PostMediaEntity {

    private Long id;

    private Long postId;

    /**
     * 1-图片，2-视频。
     */
    private Integer mediaType;

    private String url;

    private Integer sortOrder;
}
