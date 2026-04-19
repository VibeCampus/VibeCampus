package cn.ayeez.vibecampus.user.controller;

import cn.ayeez.vibecampus.user.dto.LoginRequest;
import cn.ayeez.vibecampus.user.dto.LoginResponse;
import cn.ayeez.vibecampus.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP 认证入口：路径与前端 {@code baseURL}（默认 {@code /api}）+ {@code /auth/*} 对齐。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录。
     * <ul>
     *   <li>URL：{@code POST /api/auth/login}</li>
     *   <li>Body：{@link LoginRequest}</li>
     *   <li>成功：{@link LoginResponse}；失败：由全局处理器返回 {@code { "message": "..." }}</li>
     * </ul>
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("收到登录请求，account={}", request.getAccount());
        return authService.login(request);
    }
}
