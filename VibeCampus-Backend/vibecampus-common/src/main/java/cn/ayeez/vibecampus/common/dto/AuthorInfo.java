package cn.ayeez.vibecampus.common.dto;

import lombok.Data;

/**
 * 作者信息DTO（通用）
 * <p>用于帖子、评论等场景中展示作者信息</p>
 */
@Data
public class AuthorInfo {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 头像URL */
    private String avatar;
}
