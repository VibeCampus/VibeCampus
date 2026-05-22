package cn.ayeez.vibecampus.config;

import cn.ayeez.vibecampus.user.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * JWT 认证过滤器：从 Authorization header 中提取用户ID并注入到请求属性中。
 * <p>供后续 Controller 通过 {@code request.getAttribute("currentUserId")} 获取当前登录用户ID。</p>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 公开接口和预检请求不做 JWT 校验，避免过期 token 误伤登录/注册。
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        
        // 如果没有携带 token，直接放行（由具体接口的权限控制决定是否需要认证）
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 从 token 中提取用户ID
            Long userId = jwtTokenService.extractUserIdFromToken(authorization);
            
            // 将用户ID存入请求属性，供后续使用
            request.setAttribute("currentUserId", userId);
            
            // 设置Spring Security的Authentication
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("JWT认证成功，userId={}", userId);
            filterChain.doFilter(request, response);
        } catch (ResponseStatusException ex) {
            // Token无效或已过期，返回401
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"" + ex.getReason() + "\"}");
        } catch (Exception ex) {
            log.error("JWT认证异常", ex);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"认证服务异常\"}");
        }
    }
}
