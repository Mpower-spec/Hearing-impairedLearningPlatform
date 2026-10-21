package com.hearing.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色访问控制注解。
 * 可标注在控制器类或方法上，声明访问所需的角色；由 AuthInterceptor 读取并校验。
 * 方法级注解优先于类级注解。
 */
@Target({ElementType.TYPE, ElementType.METHOD}) // 可作用于类和方法
@Retention(RetentionPolicy.RUNTIME)             // 运行时保留，供拦截器反射读取
public @interface RequireRole {
    // 允许访问的角色列表，满足其一即可
    String[] value();
}
