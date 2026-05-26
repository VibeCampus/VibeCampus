package cn.ayeez.vibecampus.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功后返回给前端的用户摘要：不包含密码、token 等敏感信息。
 * <p>嵌套在 {@link LoginResponse#getUser()} 中，与前端 {@code user} 字段对应。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    /** 用户主键 */
    private Long id;
    /** 展示用用户名 */
    private String username;
    /** 头像 URL */
    private String avatar;
    /** 当前用户可见，公开资料可按策略置空 */
    private String phone;
    /** 当前用户可见，公开资料可按策略置空 */
    private String email;
    /** 男 / 女 / 保密 / 其他 */
    private String gender;
    /** 个人简介 */
    private String bio;
    /** 专业 */
    private String major;
    /** 注册或入学时间展示字段 */
    private String joinedAt;
    /** 账号状态，扩展字段 */
    private Integer status;
    /** 是否为当前登录用户，扩展字段 */
    private Boolean isCurrentUser;

    public UserInfo(Long id, String username, String phone) {
        this.id = id;
        this.username = username;
        this.phone = phone;
    }
}
