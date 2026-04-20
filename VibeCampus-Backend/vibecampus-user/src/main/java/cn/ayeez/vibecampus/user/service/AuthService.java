package cn.ayeez.vibecampus.user.service;

import cn.ayeez.vibecampus.user.dto.LoginRequest;
import cn.ayeez.vibecampus.user.dto.LoginResponse;

/**
 * 认证领域服务：注册、登录、验证码等（本会话仅实现登录入口）。
 */
public interface AuthService {

    /**
     * 用户登录：校验验证码与密码，返回令牌与用户摘要。
     *
     * @param request 登录请求，字段已由 Controller 层 {@code @Valid} 校验非空
     * @return 非空 {@link LoginResponse}；失败时抛出 {@link org.springframework.web.server.ResponseStatusException}
     */
    LoginResponse login(LoginRequest request);
}
