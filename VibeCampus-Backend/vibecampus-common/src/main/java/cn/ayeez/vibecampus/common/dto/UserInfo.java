package cn.ayeez.vibecampus.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功后返回给前端的用户摘要：不包含密码、token 等敏感信息。
 * <p>嵌套在 {@link LoginResponse#getUser()} 中，与前端 {@code user} 字段对应。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    /** 用户主键 */
    private Long id;
    /** 展示用用户名 */
    private String username;
    /** 脱敏策略若需可在组装时处理；当前原样返回 */
    private String phone;
}
