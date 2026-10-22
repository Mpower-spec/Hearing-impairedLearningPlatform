package com.hearing.platform.logging;

import org.slf4j.MDC;

/**
 * 统一维护请求日志使用的 MDC 字段。
 */
public final class LoggingContext {

    public static final String REQUEST_ID = "requestId";
    public static final String METHOD = "method";
    public static final String PATH = "path";
    public static final String USER_ID = "userId";
    public static final String ROLE = "role";

    private LoggingContext() {
    }

    public static void startRequest(String requestId, String method, String path) {
        MDC.put(REQUEST_ID, requestId);
        MDC.put(METHOD, method);
        MDC.put(PATH, path);
    }

    public static void setUser(Long userId, String role) {
        MDC.put(USER_ID, userId == null ? "-" : userId.toString());
        MDC.put(ROLE, role == null || role.isBlank() ? "-" : role);
    }

    public static void clear() {
        MDC.clear();
    }
}
