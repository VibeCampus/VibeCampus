package cn.ayeez.vibecampus.post.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * 帖子模块当前用户上下文工具。
 */
public final class PostCurrentUserContext {

    private static final String CURRENT_USER_ID_ATTR = "currentUserId";

    private PostCurrentUserContext() {
    }

    /**
     * 获取当前登录用户 ID。
     */
    public static Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "无法获取请求上下文");
        }
        HttpServletRequest request = attributes.getRequest();
        Long userId = (Long) request.getAttribute(CURRENT_USER_ID_ATTR);
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或会话已过期");
        }
        return userId;
    }
}
