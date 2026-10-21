package com.hearing.platform.security;

import com.hearing.platform.common.BusinessException; // 业务异常

import org.springframework.http.HttpStatus;

/**
 * 当前登录用户上下文。
 * 基于 ThreadLocal 在一次请求线程内传递已认证用户，避免层层方法传参。
 * 由 AuthInterceptor 写入、并在请求结束时清理。
 */
public final class AuthContext {
    // 线程私有的当前用户持有器
    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

    // 私有构造，禁止实例化（纯静态工具类）
    private AuthContext() {
    }

    // 写入当前用户（拦截器解析令牌后调用）
    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    /**
     * 获取当前用户；若不存在（未登录）则抛出未授权异常。
     *
     * @return 当前认证用户
     */
    public static AuthUser get() {
        AuthUser user = CURRENT.get();
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    // 获取当前用户；不存在时返回 null（用于允许匿名的场景）
    public static AuthUser getOrNull() {
        return CURRENT.get();
    }

    // 清理当前线程的用户，防止线程复用导致的数据串味与内存泄漏
    public static void clear() {
        CURRENT.remove();
    }
}
