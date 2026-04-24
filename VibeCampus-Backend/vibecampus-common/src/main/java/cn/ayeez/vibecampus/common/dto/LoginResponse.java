package cn.ayeez.vibecampus.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应体：根级 {@code token} 与 {@code user}，与 axios 拦截器返回的 data 结构一致。
 * <p>若日后统一为 {@code { code, message, data }}，可改为外层包装 + 本类作为 data 内容。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** 访问令牌：JWT 字符串 */
    private String token;
    /** 当前登录用户公开信息 */
    private UserInfo user;
}
