package cn.ayeez.vibecampus.user.model;

import lombok.Data;

/**
 * 用户领域查询结果：与 MyBatis {@code users} 表映射，用于登录、资料等读模型。
 * <p>表结构变更时同步扩展字段与 {@link cn.ayeez.vibecampus.user.mapper.UserMapper} 中的 SQL。</p>
 */
@Data
public class UserProfile {

    /** 主键 */
    private Long id;
    /** 登录名 / 展示名 */
    private String username;
    /** 手机号，可与 {@link #username} 二选一作为登录账号 */
    private String phone;
    /**
     * 数据库中存储的密码摘要（如 BCrypt），永不明文返回给前端。
     */
    private String passwordHash;
    /** 用户昵称，用于前端展示 */
    private String nickname;
    /** 用户邮箱，可用于登录或找回密码 */
    private String email;
    /** 性别：0-保密/未知，1-男，2-女 */
    private Integer gender;
    /** 用户状态：0-禁用，1-启用 */
    private Integer status;
}
