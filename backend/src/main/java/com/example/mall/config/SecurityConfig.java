package com.example.mall.config;

import com.example.mall.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 安全配置
 *
 * <p>配置 JWT 无状态认证、URL 权限规则、密码编码器。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用 @PreAuthorize 方法级权限控制
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * 安全过滤器链
     *
     * <p>配置说明：
     * <ul>
     *   <li>禁用 CSRF（前后端分离 + JWT Bearer Token，天然免疫）</li>
     *   <li>无状态 Session（不创建、不使用 HttpSession）</li>
     *   <li>URL 权限规则（从上到下按优先级排列，第一条匹配的生效）</li>
     *   <li>添加 JwtAuthFilter 在 UsernamePasswordAuthenticationFilter 之前</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公共接口（无需登录）
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/brands/**").permitAll()
                        .requestMatchers("/api/upload/**").permitAll()
                        // 管理后台接口（运营及以上）
                        .requestMatchers("/api/admin/**").hasAnyRole("OPERATOR", "ADMIN")
                        // 秒杀接口（需登录）
                        .requestMatchers("/api/flash-sale/**").authenticated()
                        // 其他接口（需登录）
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器
     *
     * <p>使用 BCrypt 加密算法，strength=12（迭代次数 2^12 = 4096 轮）。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 认证管理器
     *
     * <p>用于需要手动调用认证的场景（如用户名密码登录）。
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
