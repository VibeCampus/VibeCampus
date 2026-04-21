package cn.ayeez.vibecampus.user.service.impl;

import cn.ayeez.vibecampus.user.config.JwtProperties;
import cn.ayeez.vibecampus.user.model.UserProfile;
import cn.ayeez.vibecampus.user.service.JwtTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * 基于 HMAC-SHA256 的 JWT 签发实现。
 */
@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    public JwtTokenServiceImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    void validateAndInit() {
        if (!StringUtils.hasText(jwtProperties.secret())) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JWT secret 未配置");
        }
        if (jwtProperties.expirationMinutes() <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JWT 过期时间必须大于 0");
        }
        byte[] secretBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "JWT secret 长度至少 32 字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    @Override
    public String generateAccessToken(UserProfile user) {
        Instant now = Instant.now();
        Instant expireAt = now.plus(jwtProperties.expirationMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .id(UUID.randomUUID().toString())
                .claim("username", user.getUsername())
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }
}
