package cn.ayeez.vibecampus.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 参数配置：支持通过环境变量覆盖，避免把密钥硬编码到代码中。
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes,
        String issuer
) {
}
