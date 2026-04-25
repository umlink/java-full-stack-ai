package com.example.mall.common;

/**
 * 限流异常，自动使用 RATE_LIMIT 错误码
 */
public class RateLimitException extends BusinessException {

    public RateLimitException() {
        super(BizCode.RATE_LIMIT);
    }

    public RateLimitException(String message) {
        super(BizCode.RATE_LIMIT.getCode(), message);
    }
}
