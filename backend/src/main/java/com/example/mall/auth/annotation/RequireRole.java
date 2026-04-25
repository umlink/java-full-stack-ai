package com.example.mall.auth.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义角色权限注解
 *
 * <p>基于 @PreAuthorize 实现，通过角色编码最小值控制访问权限。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 要求运营以上（运营 OR 管理员）
 * @RequireRole
 * public Result<Void> updateProduct(...) { ... }
 *
 * // 要求管理员
 * @RequireRole(min = 3)
 * public Result<Void> deleteUser(...) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole(T(com.example.mall.auth.annotation.Role).fromCode(min).name())")
public @interface RequireRole {

    /**
     * 最低角色编码要求
     * <ul>
     *   <li>1 - 普通用户（USER）</li>
     *   <li>2 - 运营（OPERATOR，默认）</li>
     *   <li>3 - 管理员（ADMIN）</li>
     * </ul>
     */
    int min() default 2;
}
