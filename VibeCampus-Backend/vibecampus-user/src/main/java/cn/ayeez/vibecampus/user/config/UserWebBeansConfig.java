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
     * 密码哈希与校验器，供登录和注册使用
     * <p>使用BCrypt算法，成本因子设置为10（默认值）</p>
     * <p>生成的哈希格式：$2a$10$... （$2a表示算法版本，10表示成本因子）</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
