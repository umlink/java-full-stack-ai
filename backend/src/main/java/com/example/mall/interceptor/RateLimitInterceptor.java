package com.example.mall.interceptor;

import com.example.mall.common.RateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 限流拦截器
 * <p>
 * 功能：
 * 1. 全局限流：基于 IP 的令牌桶限制，默认 100 req/s
 * 2. 登录接口限流：IP 维度 + 用户名维度，超限锁定
 * 3. 注册接口限流：IP 维度
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ======================== 令牌桶 Lua 脚本 ========================

    private static final String TOKEN_BUCKET_LUA =
            "local key = KEYS[1] " +
            "local capacity = tonumber(ARGV[1]) " +
            "local refillRate = tonumber(ARGV[2]) " +
            "local now = tonumber(ARGV[3]) " +
            "local state = redis.call('HMGET', key, 'tokens', 'lastRefill') " +
            "local tokens = tonumber(state[1]) or capacity " +
            "local lastRefill = tonumber(state[2]) or now " +
            "local elapsed = math.max(0, now - lastRefill) " +
            "tokens = math.min(capacity, tokens + elapsed * refillRate) " +
            "if tokens >= 1 then " +
            "  redis.call('HMSET', key, 'tokens', tokens - 1, 'lastRefill', now) " +
            "  redis.call('EXPIRE', key, 10) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>(TOKEN_BUCKET_LUA, Long.class);

    // ======================== 限流常量 ========================

    /** 全局限流：令牌桶容量，即最大突发请求数 */
    private static final long GLOBAL_CAPACITY = 100;

    /** 全局限流：令牌每秒补充速率 */
    private static final double GLOBAL_REFILL_RATE = 100.0;

    /** 登录-IP维度：60 秒内最多 10 次 */
    private static final long LOGIN_IP_MAX = 10;
    private static final long LOGIN_IP_TTL = 60;

    /** 登录-用户名维度：900 秒（15 分钟）内最多 5 次失败 */
    private static final long LOGIN_USER_MAX = 5;
    private static final long LOGIN_USER_TTL = 900;

    /** 锁定时间：900 秒（15 分钟） */
    private static final long LOCK_TTL = 900;

    /** 注册-IP维度：3600 秒（1 小时）内最多 3 次 */
    private static final long REGISTER_IP_MAX = 3;
    private static final long REGISTER_IP_TTL = 3600;

    // ======================== 路径匹配 ========================

    private static final List<String> LOGIN_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/login"
    );

    private static final List<String> REGISTER_PATHS = Arrays.asList(
            "/api/auth/register",
            "/api/register"
    );

    // ======================== 拦截入口 ========================

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIP(request);
        String path = request.getRequestURI();

        // 1. 全局限流（令牌桶）
        if (!checkGlobalRateLimit(ip)) {
            throw new RateLimitException("请求过于频繁，请稍后再试");
        }

        // 2. 登录接口限流
        if (isLoginPath(path)) {
            // 优先从请求参数获取用户名，因为当前请求认证尚未发生，
            // 无法从 SecurityContext 中获取
            String username = request.getParameter("username");
            if (username != null && !username.isEmpty()) {
                checkLoginRateLimit(ip, username);
            } else {
                // 无 username 参数时，仅做 IP 维度限流
                checkLoginIpRateLimit(ip);
            }
        }

        // 3. 注册接口限流
        if (isRegisterPath(path)) {
            checkRegisterRateLimit(ip);
        }

        return true;
    }

    // ======================== 全局限流 ========================

    /**
     * 基于令牌桶算法的全局限流。
     * Redis Hash 结构：{@code rate:global:<ip>} 存储令牌数和最后补充时间。
     *
     * @param ip 客户端 IP
     * @return true 允许放行，false 触发限流
     */
    private boolean checkGlobalRateLimit(String ip) {
        String key = "rate:global:" + ip;
        Long result = stringRedisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(key),
                String.valueOf(GLOBAL_CAPACITY),
                String.valueOf(GLOBAL_REFILL_RATE),
                String.valueOf(Instant.now().getEpochSecond())
        );
        return result != null && result == 1L;
    }

    // ======================== 登录限流（IP + 用户名） ========================

    /**
     * 登录接口限流（含用户名维度）。
     * <ol>
     *   <li>IP 维度：{@code rate:login:<ip>} EXP 60s，60s 内最多 10 次</li>
     *   <li>用户名维度：{@code rate:login:user:<username>} EXP 900s，15min 内最多 5 次</li>
     *   <li>超限锁定：{@code login:locked:<username>} EX 900，锁定期间拒绝所有请求</li>
     * </ol>
     */
    private void checkLoginRateLimit(String ip, String username) {
        // ---- 检查是否已锁定 ----
        String lockKey = "login:locked:" + username;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey))) {
            throw new RateLimitException("账户已被锁定，请15分钟后重试");
        }

        // ---- IP 维度 ----
        checkLoginIpRateLimit(ip);

        // ---- 用户名维度（失败次数计数） ----
        String userKey = "rate:login:user:" + username;
        Long userCount = stringRedisTemplate.opsForValue().increment(userKey);
        if (userCount == 1) {
            stringRedisTemplate.expire(userKey, LOGIN_USER_TTL, TimeUnit.SECONDS);
        }
        if (userCount > LOGIN_USER_MAX) {
            // 超过失败次数上限，锁定账户
            stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL, TimeUnit.SECONDS);
            throw new RateLimitException("登录失败次数过多，账户已被锁定，请15分钟后重试");
        }
    }

    /**
     * 登录接口 IP 维度限流。
     * 使用 INCR + EXPIRE 原子操作，固定窗口计数器。
     */
    private void checkLoginIpRateLimit(String ip) {
        String ipKey = "rate:login:" + ip;
        Long ipCount = stringRedisTemplate.opsForValue().increment(ipKey);
        if (ipCount == 1) {
            stringRedisTemplate.expire(ipKey, LOGIN_IP_TTL, TimeUnit.SECONDS);
        }
        if (ipCount > LOGIN_IP_MAX) {
            throw new RateLimitException("登录尝试过于频繁，请稍后再试");
        }
    }

    // ======================== 注册限流 ========================

    /**
     * 注册接口 IP 维度限流。
     * 使用 INCR + EXPIRE 原子操作，固定窗口计数器。
     */
    private void checkRegisterRateLimit(String ip) {
        String key = "rate:register:" + ip;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == 1) {
            stringRedisTemplate.expire(key, REGISTER_IP_TTL, TimeUnit.SECONDS);
        }
        if (count > REGISTER_IP_MAX) {
            throw new RateLimitException("注册尝试过于频繁，请稍后再试");
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 判断请求路径是否为登录接口。
     */
    private boolean isLoginPath(String path) {
        return LOGIN_PATHS.stream().anyMatch(p -> path.equalsIgnoreCase(p));
    }

    /**
     * 判断请求路径是否为注册接口。
     */
    private boolean isRegisterPath(String path) {
        return REGISTER_PATHS.stream().anyMatch(p -> path.equalsIgnoreCase(p));
    }

    /**
     * 从请求中获取客户端真实 IP。
     * <ul>
     *   <li>优先读取 X-Forwarded-For 头（代理转发场景）</li>
     *   <li>其次 X-Real-IP 头</li>
     *   <li>最后回退到 getRemoteAddr()</li>
     * </ul>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含逗号分隔的多级代理 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
