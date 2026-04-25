package com.example.mall.auth;

import com.example.mall.auth.annotation.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * JWT 认证过滤器
 *
 * <p>从 Authorization header 提取 Bearer token，校验后将用户信息注入 SecurityContextHolder。
 * 白名单路径直接放行，校验失败返回 401 JSON 响应。
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    /**
     * 白名单路径（不需要 JWT 认证）
     */
    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/auth/**",
            "/api/products/**",
            "/api/categories/**",
            "/api/brands/**",
            "/api/upload/**",
            "/error"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    /**
     * 判断当前请求路径是否匹配白名单
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return SKIP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        // 无 Authorization header，直接放行（后续由 SecurityConfig 控制权限）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Token 校验失败，返回 401
        if (!jwtProvider.validateToken(token)) {
            log.debug("无效的 JWT token: {}", token);
            writeUnauthorizedResponse(response);
            return;
        }

        try {
            Long userId = jwtProvider.getUserIdFromToken(token);
            Integer roleCode = jwtProvider.getRoleFromToken(token);

            // 构造角色权限：需加 ROLE_ 前缀以匹配 hasRole() 方法
            String roleName = Role.fromCode(roleCode).name();
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + roleName)
            );

            // 构造认证令牌（principal=userId, credentials=null, authorities=角色）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 注入 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("JWT token 解析失败: {}", e.getMessage());
            writeUnauthorizedResponse(response);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 写入 401 未授权 JSON 响应
     */
    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":401,\"message\":\"UNAUTHORIZED\",\"data\":null}"
        );
    }
}
