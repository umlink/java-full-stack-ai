package com.example.mall.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发/校验工具类
 *
 * <p>使用 jjwt 0.12 库，HMAC-SHA256 签名算法
 */
@Component
public class JwtProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtProvider.class);

    /**
     * Access Token 有效期：2 小时（单位：毫秒）
     */
    private static final long ACCESS_TOKEN_EXPIRATION_MS = 7200 * 1000L;

    /**
     * 开发环境默认密钥（仅开发环境使用，生产环境必须通过 JWT_SECRET 环境变量配置）
     */
    private static final String DEFAULT_SECRET = "YourJwtSecretKeyForDevelopmentOnlyDoNotUseInProduction123!";

    private final SecretKey key;

    /**
     * 从环境变量 JWT_SECRET 读取密钥，若为空则使用默认密钥（仅开发环境）
     */
    public JwtProvider(@Value("${JWT_SECRET:}") String jwtSecret) {
        String secret = jwtSecret;
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET 环境变量未设置，使用默认密钥（仅限开发环境）");
            secret = DEFAULT_SECRET;
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param role     角色编码（1-普通用户, 2-运营, 3-管理员）
     * @return JWT token 字符串
     */
    public String generateToken(Long userId, String username, Integer role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ACCESS_TOKEN_EXPIRATION_MS);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * 从 Token 中提取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * 从 Token 中提取角色编码
     */
    public Integer getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", Integer.class);
    }

    /**
     * 校验 Token 是否有效（签名正确且未过期）
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT token 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析并验证 Token，返回 Claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
