package com.example.mall.auth.annotation;

/**
 * 角色枚举
 *
 * <p>与 user 表中 role 字段对应：1-普通用户, 2-运营, 3-管理员
 */
public enum Role {
    USER(1),
    OPERATOR(2),
    ADMIN(3);

    private final int code;

    Role(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据编码获取对应的角色枚举
     *
     * @param code 角色编码
     * @return 角色枚举
     * @throws IllegalArgumentException 当编码无效时抛出
     */
    public static Role fromCode(int code) {
        for (Role role : Role.values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("无效的角色编码: " + code);
    }
}
