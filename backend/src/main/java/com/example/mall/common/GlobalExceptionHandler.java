package com.example.mall.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理
 * 统一切换异常为 JSON 响应，记录日志
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * BusinessException → 400
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * RateLimitException → 429
     */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleRateLimitException(RateLimitException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * AuthenticationException → 401
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return Result.error(BizCode.UNAUTHORIZED);
    }

    /**
     * AccessDeniedException → 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return Result.error(BizCode.FORBIDDEN);
    }

    /**
     * MethodArgumentNotValidException → 422，返回字段级错误列表
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Result<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        List<Map<String, String>> fields = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            Map<String, String> fieldErrorMap = new HashMap<>();
            fieldErrorMap.put("field", fieldError.getField());
            fieldErrorMap.put("message", fieldError.getDefaultMessage());
            fields.add(fieldErrorMap);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("fields", fields);
        return Result.error(BizCode.VALIDATION_ERROR.getCode(), BizCode.VALIDATION_ERROR.getMessage(), data);
    }

    /**
     * MissingServletRequestParameterException → 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParamException(MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getMessage());
        return Result.error(BizCode.PARAM_MISSING.getCode(), "缺少必要参数: " + e.getParameterName());
    }

    /**
     * HttpMessageNotReadableException → 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("Message not readable: {}", e.getMessage());
        // 处理 JSON 解析异常，提取更友好的提示
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String fieldName = ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .reduce((first, second) -> second)
                    .orElse("unknown");
            String msg = String.format("参数格式错误: %s 的值不正确", fieldName);
            return Result.error(BizCode.PARAM_ERROR.getCode(), msg);
        }
        return Result.error(BizCode.PARAM_ERROR);
    }

    /**
     * 未捕获异常 → 500
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknownException(Exception e) {
        log.error("Unexpected error: ", e);
        return Result.error(BizCode.SYSTEM_ERROR);
    }
}
