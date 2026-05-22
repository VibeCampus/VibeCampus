package cn.ayeez.vibecampus.user.controller;

import cn.ayeez.vibecampus.common.dto.LoginRequest;
import cn.ayeez.vibecampus.common.dto.LoginResponse;
import cn.ayeez.vibecampus.common.dto.RegisterRequest;
import cn.ayeez.vibecampus.user.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP 认证入口：路径与前端 {@code baseURL}（默认 {@code /api}）+ {@code /auth/*} 对齐。
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/captcha")
    public Map<String, String> captcha() {
        String captcha = randomCaptcha();
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="112" height="40" viewBox="0 0 112 40">
                  <rect width="112" height="40" fill="#f6f8fb"/>
                  <line x1="8" y1="31" x2="104" y2="9" stroke="#d7dde8" stroke-width="2"/>
                  <line x1="4" y1="10" x2="108" y2="30" stroke="#e3e8f1" stroke-width="2"/>
                  <text x="56" y="27" text-anchor="middle" font-family="Consolas, Monaco, monospace" font-size="22" font-weight="700" letter-spacing="4" fill="#1772f6">%s</text>
                </svg>
                """.formatted(captcha);
        String image = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return Map.of(
                "captchaId", UUID.randomUUID().toString(),
                "image", image
        );
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

    /**
     * 用户注册接口
     * <ul>
     *   <li>URL：{POST /api/auth/register}</li>
     *   <li>Body：{RegisterRequest}，包含用户名、密码等信息</li>
     *   <li>密码处理：使用BCrypt算法进行哈希加密，成本因子为10</li>
     *   <li>成功：返回 {@link LoginResponse}（含 token 与 user），HTTP 200</li>
     *   <li>失败：由全局处理器返回  { "message": "..." }}</li>
     * </ul>
     *
     * request 注册请求体，经过 @Valid 校验
     * return 包含新用户ID和用户名的响应
     */
    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        log.info("收到注册请求，username={}", request.getUsername());
        return authService.register(request);
    }

    /**
     * 用户登出：使当前 Bearer Token 失效。
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request.getHeader("Authorization"));
        return ResponseEntity.ok().build();
    }

    private String randomCaptcha() {
        StringBuilder captcha = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            captcha.append(CAPTCHA_CHARS.charAt(RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        return captcha.toString();
    }
}
