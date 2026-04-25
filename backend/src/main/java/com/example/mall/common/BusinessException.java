package com.example.mall.common;

/**
 * 业务异常，继承 RuntimeException，包含 code 和 message
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(BizCode bizCode) {
        super(bizCode.getMessage());
        this.code = bizCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
