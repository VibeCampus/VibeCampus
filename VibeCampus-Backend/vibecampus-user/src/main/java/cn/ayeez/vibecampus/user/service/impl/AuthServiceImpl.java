package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.user.dto.LoginRequest;
import cn.ayeez.vibecampus.user.dto.LoginResponse;
import cn.ayeez.vibecampus.user.dto.UserInfo;
import cn.ayeez.vibecampus.user.mapper.UserMapper;
import cn.ayeez.vibecampus.user.model.UserProfile;
import cn.ayeez.vibecampus.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@link AuthService} 参考实现：验证码占位、按账号查库、BCrypt 验密、占位 token。
 * <p>异常使用 {@link ResponseStatusException}，由 bootstrap 中 {@code ApiExceptionHandler} 转为带 {@code message} 的 JSON。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1) 验证码（当前仅非空校验，避免与图形验证码接口脱节）
        assertCaptcha(request.getCaptcha());

        log.info("用户登录尝试，account={}", request.getAccount());
        // 2) 按用户名或手机号查一条用户
        UserProfile user = userMapper.selectByAccount(request.getAccount().trim());
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            // 用户不存在或无密码哈希：统一提示，避免枚举有效账号
            log.info("登录失败：用户不存在或未设置密码，account={}", request.getAccount());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        
        // DEBUG: 输出数据库中的密码哈希值用于排查问题
        log.debug("=== DEBUG 信息 ===");
        log.debug("从数据库读取的 password_hash: [{}]", user.getPasswordHash());
        log.debug("password_hash 长度: {}", user.getPasswordHash().length());
        log.debug("是否以 $2a$ 开头: {}", user.getPasswordHash().startsWith("$2a$"));
        log.debug("输入的明文密码: [{}]", request.getPassword());
        log.debug("==================");
        
        // 3) 明文密码与库中 password_hash 比对
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.info("登录失败：密码错误，userId={}", user.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        log.info("登录成功，userId={}", user.getId());

        // TODO: 使用 jjwt / spring-security-oauth2-resource-server 签发 JWT，并设置过期、jti、刷新策略等
        String token = "stub-token-replace-with-jwt";

        UserInfo info = new UserInfo(user.getId(), user.getUsername(), user.getPhone());
        return new LoginResponse(token, info);
    }

    /**
     * 验证码校验占位：非空即可通过。
     * <p>生产环境应与 {@code GET /auth/captcha} 下发的 captchaId 及 Redis/Session 中正确答案比对。</p>
     */
    private void assertCaptcha(String captcha) {
        if (captcha == null || captcha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不能为空");
        }
        // TODO: captchaId + 缓存答案校验
    }
}
