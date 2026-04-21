package cn.ayeez.vibecampus.user.service;

import cn.ayeez.vibecampus.user.dto.LoginRequest;
import cn.ayeez.vibecampus.user.dto.LoginResponse;
import cn.ayeez.vibecampus.user.dto.RegisterRequest;

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

    /**
     * 用户注册：验证输入信息，对密码进行BCrypt哈希加密（成本因子10），创建新用户账户
     *
     * @param request 注册请求，包含用户名、密码等基本信息
     * @return 注册成功后返回令牌与用户摘要
     */
    LoginResponse register(RegisterRequest request);

    /**
     * 用户登出：使当前 access token 失效。
     */
    void logout(String authorizationHeader);
}
