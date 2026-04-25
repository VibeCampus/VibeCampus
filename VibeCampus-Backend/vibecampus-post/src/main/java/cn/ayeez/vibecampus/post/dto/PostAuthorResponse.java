package cn.ayeez.vibecampus.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子作者摘要信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostAuthorResponse {

    private Long id;

    private String username;

    private String avatar;
}
