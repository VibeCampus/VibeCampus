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
    private final JwtTokenService jwtTokenService;

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
        // 3) 明文密码与库中 password_hash 比对
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.info("登录失败：密码错误，userId={}", user.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        log.info("登录成功，userId={}", user.getId());

        String token = jwtTokenService.generateAccessToken(user);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        // 1) 参数预处理：去除首尾空格
        String username = request.getUsername().trim();
        String password = request.getPassword();
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String nickname = request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname().trim() : username;
        log.info("用户注册，username={}, phone={}, email={}, nickname={}", username, phone, email, nickname);

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

        // 8) 返回用户ID
        Long userId = newUser.getId();
        log.info("用户注册成功，userId={}, username={}", userId,  username);

        return userId;
    }
}
