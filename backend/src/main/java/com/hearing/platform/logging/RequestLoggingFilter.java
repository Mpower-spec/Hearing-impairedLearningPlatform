package com.hearing.platform.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * API 访问日志过滤器。
 * 只记录请求方法、路径、状态码、耗时和已认证用户，不记录请求参数、请求头或请求体。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger accessLog = LoggerFactory.getLogger("com.hearing.platform.access");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!path.equals("/api") && !path.startsWith("/api/")) {
            return true;
        }
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        LoggingContext.startRequest(UUID.randomUUID().toString(), request.getMethod(), request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException ex) {
            markServerErrorIfNeeded(response);
            throw ex;
        } catch (RuntimeException ex) {
            markServerErrorIfNeeded(response);
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            accessLog.info("method={} path={} status={} durationMs={} userId={} role={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    mdcValue(LoggingContext.USER_ID),
                    mdcValue(LoggingContext.ROLE));
            LoggingContext.clear();
        }
    }

    private void markServerErrorIfNeeded(HttpServletResponse response) {
        if (response.getStatus() < 400) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String mdcValue(String key) {
        String value = MDC.get(key);
        return value == null || value.isBlank() ? "-" : value;
    }
}
