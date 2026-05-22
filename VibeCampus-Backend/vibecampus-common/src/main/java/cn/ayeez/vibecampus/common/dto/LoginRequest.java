package cn.ayeez.vibecampus.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求体：与前端 {@code authApi.login}、接口文档 {@code POST /auth/login} 对齐。
 * <p>使用 Bean Validation，校验失败由全局异常处理转为带 {@code message} 的 400 响应。</p>
 */
@Data
public class LoginRequest {

    /** 用户名或手机号，由业务层与库字段匹配 */
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 明文密码，仅用于与库中哈希比对，禁止记录到日志 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 图形验证码用户输入；后续可与 captchaId + 缓存答案校验 */
    @NotBlank(message = "验证码不能为空")
    private String captcha;

    /** 验证码会话标识：与 GET /auth/captcha 返回的 captchaId 配对 */
    @NotBlank(message = "captchaId不能为空")
    private String captchaId;
}
