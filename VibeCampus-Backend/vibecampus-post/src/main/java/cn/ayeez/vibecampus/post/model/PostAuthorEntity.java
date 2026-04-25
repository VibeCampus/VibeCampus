package cn.ayeez.vibecampus.post.model;

import lombok.Data;

/**
 * 帖子作者查询实体。
 */
@Data
public class PostAuthorEntity {

    private Long id;

    private String username;

    private String avatar;
}
