package com.example.mall.common;

/**
 * 业务错误码枚举，按模块划分
 */
public enum BizCode {

    // ========== 通用 ==========
    SUCCESS(200, "success"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试"),
    VALIDATION_ERROR(422, "VALIDATION_ERROR"),
    PARAM_ERROR(400, "请求参数格式错误"),
    PARAM_MISSING(400, "缺少必要参数"),

    // ========== 认证模块 (40xxx) ==========
    UNAUTHORIZED(401, "登录已过期，请重新登录"),
    FORBIDDEN(403, "没有权限访问"),
    PASSWORD_ERROR(40101, "密码错误"),
    USER_NOT_FOUND(40102, "用户不存在"),
    ACCOUNT_DISABLED(40301, "账号已被禁用"),
    USER_EXISTS(40001, "该用户名已被注册"),
    OLD_PASSWORD_ERROR(40002, "旧密码错误"),
    PASSWORD_SAME(40003, "新密码与旧密码不能相同"),

    // ========== 商品模块 (20xxx) ==========
    PRODUCT_NOT_FOUND(20001, "商品不存在"),
    PRODUCT_OFFLINE(20002, "商品已下架"),
    STOCK_NOT_ENOUGH(20003, "库存不足"),

    // ========== 地址模块 (25xxx) ==========
    ADDRESS_NOT_FOUND(25001, "地址不存在"),
    ADDRESS_LIMIT(25002, "最多添加 20 个地址"),

    // ========== 购物车模块 (26xxx) ==========
    CART_NOT_FOUND(26001, "购物车项不存在"),
    CART_QUANTITY_EXCEED(26002, "单种商品最多购买99件"),
    CART_SKU_REQUIRED(26003, "多规格商品请选择规格"),

    // ========== 订单模块 (30xxx) ==========
    ORDER_NOT_FOUND(30001, "订单不存在"),
    ORDER_TIMEOUT(30002, "订单已超时"),
    DUPLICATE_ORDER(30003, "请勿重复提交订单"),
    ORDER_CANNOT_CANCEL(30004, "当前订单状态不可取消"),
    ORDER_ALREADY_PAID(30005, "订单已支付"),
    ORDER_ALREADY_SHIPPED(30006, "订单已发货"),

    // ========== 秒杀模块 (40xxx) ==========
    FLASH_SALE_NOT_STARTED(40001, "秒杀尚未开始"),
    FLASH_SALE_ENDED(40002, "秒杀已结束"),
    FLASH_SALE_LIMIT(40003, "您已抢购过该商品"),
    SOLD_OUT(40004, "已售罄"),

    // ========== 限流 ==========
    RATE_LIMIT(429, "操作太频繁，请稍后再试");

    private final int code;
    private final String message;

    BizCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
