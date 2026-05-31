package cn.ayeez.vibecampus.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * 当前用户上下文工具类：从请求属性中提取由 JWT 认证过滤器注入的用户 ID。
 */
public final class CurrentUserContext {

    private static final String CURRENT_USER_ID_ATTR = "currentUserId";

    private CurrentUserContext() {
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID
     * @throws ResponseStatusException 如果未登录或 token 无效
     */
    public static Long getCurrentUserId() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法获取请求上下文");
        }

        HttpServletRequest request = attributes.getRequest();
        Long userId = (Long) request.getAttribute(CURRENT_USER_ID_ATTR);

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或会话已过期");
        }

        return userId;
    }

    /**
     * 获取当前登录用户 ID，可选，允许未登录。
     *
     * @return 用户 ID，未登录时返回 null
     */
    public static Long getCurrentUserIdOptional() {
        try {
            return getCurrentUserId();
        }
        catch (ResponseStatusException e) {
            return null;
        }
    }
}
