package cn.ayeez.vibecampus.config;

import cn.ayeez.vibecampus.user.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * JWT 吊销校验过滤器：当请求携带 Bearer token 时，拦截已吊销令牌。
 */
@Component
public class JwtRevocationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtRevocationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            if (jwtTokenService.isAccessTokenRevoked(authorization)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"Token已失效，请重新登录\"}");
                return;
            }
        }
        catch (ResponseStatusException ex) {
            response.setStatus(ex.getStatusCode().value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"" + ex.getReason() + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
