package cn.ayeez.vibecampus.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 用户模块 Web 相关 Bean：仅引入 {@code spring-security-crypto}，不启用完整 Spring Security 过滤器链。
 * <p>{@link PasswordEncoder} 供登录时对 {@code password_hash} 做 BCrypt 比对。</p>
 */
@Configuration
public class UserWebBeansConfig {

    /**
     * 密码哈希与校验器，与 {@link cn.ayeez.vibecampus.user.service.impl.AuthServiceImpl} 注入使用。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
