package com.example.mall.common;

import java.time.Instant;

/**
 * 统一返回体
 */
public class Result<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    private Result() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toEpochMilli();
    }

    /**
     * 成功（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(BizCode.SUCCESS.getCode(), BizCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(BizCode.SUCCESS.getCode(), BizCode.SUCCESS.getMessage(), data);
    }

    /**
     * 自定义错误（code + message）
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 使用业务错误码
     */
    public static <T> Result<T> error(BizCode bizCode) {
        return new Result<>(bizCode.getCode(), bizCode.getMessage(), null);
    }

    /**
     * 自定义错误（带数据体）
     */
    public static <T> Result<T> error(int code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 分页返回
     */
    public static <T> Result<PageResult<T>> page(java.util.List<T> records, long total, int page, int pageSize) {
        PageResult<T> pageResult = new PageResult<>(records, total, page, pageSize);
        return new Result<>(BizCode.SUCCESS.getCode(), BizCode.SUCCESS.getMessage(), pageResult);
    }

    /**
     * 从 MyBatis-Plus IPage 构造分页返回
     */
    public static <T> Result<PageResult<T>> page(com.baomidou.mybatisplus.core.metadata.IPage<T> ipage) {
        PageResult<T> pageResult = PageResult.of(ipage);
        return new Result<>(BizCode.SUCCESS.getCode(), BizCode.SUCCESS.getMessage(), pageResult);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
