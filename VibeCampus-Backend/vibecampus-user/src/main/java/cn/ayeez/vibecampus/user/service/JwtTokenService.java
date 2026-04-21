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
}
