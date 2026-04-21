package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.user.dto.LoginRequest;
import cn.ayeez.vibecampus.user.dto.LoginResponse;
import cn.ayeez.vibecampus.user.dto.RegisterRequest;
import cn.ayeez.vibecampus.user.dto.UserInfo;
import cn.ayeez.vibecampus.user.mapper.UserMapper;
import cn.ayeez.vibecampus.user.model.UserProfile;
import cn.ayeez.vibecampus.user.service.AuthService;
import cn.ayeez.vibecampus.user.service.JwtTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AuthService} 参考实现：验证码占位、按账号查库、BCrypt 验密、占位 token。
 * <p>异常使用 {@link ResponseStatusException}，由 bootstrap 中 {@code ApiExceptionHandler} 转为带 {@code message} 的 JSON。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);
    private static final ConcurrentHashMap<String, LoginThrottleState> LOGIN_THROTTLES = new ConcurrentHashMap<>();

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1) 验证码（当前仅非空校验，避免与图形验证码接口脱节）
        assertCaptcha(request.getCaptcha(), request.getCaptchaId());
        String account = request.getAccount().trim();
        String throttleKey = buildThrottleKey(account);
        assertNotLocked(throttleKey);

        log.info("用户登录尝试，account={}", account);
        // 2) 按用户名或手机号查一条用户
        UserProfile user = userMapper.selectByAccount(account);
        if (user == null || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            // 用户不存在或无密码哈希：统一提示，避免枚举有效账号
            onLoginFailure(throttleKey);
            log.info("登录失败：用户不存在或未设置密码，account={}", account);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        // 3) 明文密码与库中 password_hash 比对
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            onLoginFailure(throttleKey);
            log.info("登录失败：密码错误，userId={}", user.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        LOGIN_THROTTLES.remove(throttleKey);
        log.info("登录成功，userId={}", user.getId());

        String token = jwtTokenService.generateAccessToken(user);

        UserInfo info = new UserInfo(user.getId(), user.getUsername(), user.getPhone());
        return new LoginResponse(token, info);
    }

    /**
     * 验证码校验占位：非空即可通过。
     * <p>生产环境应与 {@code GET /auth/captcha} 下发的 captchaId 及 Redis/Session 中正确答案比对。</p>
     */
    private void assertCaptcha(String captcha, String captchaId) {
        if (captcha == null || captcha.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不能为空");
        }
        if (captchaId == null || captchaId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "captchaId不能为空");
        }
        // TODO: captchaId + 缓存答案校验
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        if (request.getCaptcha() == null || request.getCaptcha().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不能为空");
        }
        // 1) 参数预处理：去除首尾空格
        String username = request.getUsername().trim();
        String password = request.getPassword();
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String nickname = request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname().trim() : username;
        log.info("用户注册，username={}, phone={}, email={}, nickname={}",
                username, maskPhone(phone), maskEmail(email), nickname);

        // 2) 检查用户名是否唯一
        UserProfile existingUser = userMapper.selectByAccount(username);
        if (existingUser != null) {
            log.info("注册失败：用户名已存在，username={}", username);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名已存在");
        }
        // 3) 检查手机号是否唯一
        if (phone != null && !phone.isEmpty()) {
            UserProfile existingPhone = userMapper.selectByAccount(phone);
            if (existingPhone != null) {
                log.info("注册失败：手机号已被注册，phone={}", phone);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号已被注册");
            }
        }
        // 4) 检查邮箱是否唯一
        if (email != null && !email.isEmpty()) {
            UserProfile existingEmail = userMapper.selectByAccount(email);
            if (existingEmail != null) {
                log.info("注册失败：邮箱已被注册，email={}", email);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱已被注册");
            }
        }

        // 5) 对密码进行BCrypt哈希
        // BCryptPasswordEncoder默认使用成本因子10，生成的哈希格式为：$2a$10$...
        // - $2a$: BCrypt算法标识
        // - 10: 成本因子（2^10 = 1024次迭代）
        // - 后续部分：盐值和哈希结果
        String passwordHash = passwordEncoder.encode(password);
        log.info("用户密码已加密，passwordHash={}", passwordHash.substring(0, 7)); // 仅日志前7字符，避免泄露敏感信息

        // 6) 创建用户实体对象
        UserProfile newUser = new UserProfile();
        newUser.setUsername(username);
        newUser.setPasswordHash(passwordHash);
        newUser.setPhone(phone);
        newUser.setEmail(email);
        newUser.setNickname(nickname);
        newUser.setGender(parseGender(request.getGender()));
        // newUser.setStatus(1);
        // status默认为1（启用状态），在SQL中已设置

        // 7) 插入用户记录
        try {
            int rowsAffected = userMapper.insertUser(newUser);
            if (rowsAffected <= 0) {
                log.error("注册失败：插入用户记录失败，username={}", username);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "注册失败，请稍后重试");
            }
        }
        catch (org.springframework.dao.DuplicateKeyException e) {
            // 捕获数据库唯一约束冲突异常（并发注册场景）
            log.warn("注册失败：违反唯一约束（可能是用户名/手机号/邮箱重复），username={}", username);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名、手机号或邮箱已被注册");
        }

        // 8) 返回 token + user
        Long userId = newUser.getId();
        log.info("用户注册成功，userId={}, username={}", userId, username);
        String token = jwtTokenService.generateAccessToken(newUser);
        UserInfo info = new UserInfo(newUser.getId(), newUser.getUsername(), newUser.getPhone());
        return new LoginResponse(token, info);
    }

    @Override
    public void logout(String authorizationHeader) {
        jwtTokenService.revokeAccessToken(authorizationHeader);
    }

    private String buildThrottleKey(String account) {
        return account.toLowerCase(Locale.ROOT) + "|" + resolveClientIp();
    }

    private void assertNotLocked(String throttleKey) {
        LoginThrottleState state = LOGIN_THROTTLES.get(throttleKey);
        if (state == null || state.lockUntil == null) {
            return;
        }
        if (state.lockUntil.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁，请稍后重试");
        }
        LOGIN_THROTTLES.remove(throttleKey);
    }

    private void onLoginFailure(String throttleKey) {
        LOGIN_THROTTLES.compute(throttleKey, (key, state) -> {
            Instant now = Instant.now();
            LoginThrottleState current = state == null ? new LoginThrottleState() : state;
            if (current.lockUntil != null && current.lockUntil.isBefore(now)) {
                current.failures = 0;
                current.lockUntil = null;
            }
            current.failures++;
            if (current.failures >= MAX_LOGIN_FAILURES) {
                current.lockUntil = now.plus(LOGIN_LOCK_DURATION);
            }
            return current;
        });
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return "unknown";
        }
        String forwarded = attributes.getRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[0].trim();
        }
        return attributes.getRequest().getRemoteAddr();
    }

    private Integer parseGender(String gender) {
        if (gender == null || gender.isBlank() || "保密".equals(gender)) {
            return 0;
        }
        if ("男".equals(gender)) {
            return 1;
        }
        if ("女".equals(gender)) {
            return 2;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender取值非法");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        if (name.length() <= 2) {
            return "*@" + parts[1];
        }
        return name.substring(0, 2) + "***@" + parts[1];
    }

    private static class LoginThrottleState {
        private int failures;
        private Instant lockUntil;
    }
}
