package cn.ayeez.vibecampus.user.service;

import cn.ayeez.vibecampus.user.model.UserProfile;

/**
 * 令牌签发服务：封装 JWT 生成，供认证流程调用。
 */
public interface JwtTokenService {

    /**
     * 为指定用户签发访问令牌。
     */
    String generateAccessToken(UserProfile user);

    /**
     * 吊销 access token（用于登出）。
     */
    void revokeAccessToken(String authorizationHeader);

    /**
     * 校验 token 是否已被吊销。
     */
    boolean isAccessTokenRevoked(String authorizationHeader);

    /**
     * 从 Authorization header 中提取并解析用户ID。
     *
     * @param authorizationHeader Bearer token
     * @return 用户ID
     * @throws org.springframework.web.server.ResponseStatusException 如果token无效或已过期
     */
    Long extractUserIdFromToken(String authorizationHeader);
}
