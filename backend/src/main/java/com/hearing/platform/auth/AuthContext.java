package com.hearing.platform.auth;

import com.hearing.platform.common.BusinessException;
import org.springframework.http.HttpStatus;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    public static AuthUser get() {
        AuthUser user = CURRENT.get();
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    public static AuthUser getOrNull() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}