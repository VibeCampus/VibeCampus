package cn.ayeez.vibecampus.user.controller;

import cn.ayeez.vibecampus.user.dto.LoginRequest;
import cn.ayeez.vibecampus.user.dto.LoginResponse;
import cn.ayeez.vibecampus.user.dto.RegisterRequest;
import cn.ayeez.vibecampus.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * 用户注册接口
     * <ul>
     *   <li>URL：{POST /api/auth/register}</li>
     *   <li>Body：{RegisterRequest}，包含用户名、密码等信息</li>
     *   <li>密码处理：使用BCrypt算法进行哈希加密，成本因子为10</li>
     *   <li>成功：返回 { "userId": 123, "username": "xxx" }}，HTTP 200</li>
     *   <li>失败：由全局处理器返回  { "message": "..." }}</li>
     * </ul>
     *
     * request 注册请求体，经过 @Valid 校验
     * return 包含新用户ID和用户名的响应
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("收到注册请求，username={}", request.getUsername());

        // 调用服务层执行注册逻辑（包括密码BCrypt哈希加密）
        Long userId = authService.register(request);

        // 构建响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("username", request.getUsername());
        response.put("message", "注册成功");

        // 返回HTTP 200 状态码
        return ResponseEntity.ok(response);
    }
}
