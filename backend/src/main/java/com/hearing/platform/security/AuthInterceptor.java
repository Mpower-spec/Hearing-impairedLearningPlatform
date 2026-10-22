package com.hearing.platform.security;

import com.hearing.platform.common.BusinessException; // 业务异常（携带 HTTP 状态）
import com.hearing.platform.logging.LoggingContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;   // 被调用的控制器方法元信息
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 认证与鉴权拦截器。
 * 对 /api/** 生效（WebConfig 中注册），负责：
 * 1) 解析并校验请求头中的 JWT，写入 AuthContext；
 * 2) 依据 @RequireRole 做角色权限校验。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    // JWT 服务，用于解析令牌，构造器注入
    private final JwtService jwtService;

    // 构造器注入 JwtService
    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * 请求进入控制器前执行：鉴权 + 权限校验。
     *
     * @return true 放行；抛出异常则中断请求
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 预检请求（CORS OPTIONS）直接放行，无需携带令牌
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 读取 Authorization 头，必须为 "Bearer <token>" 格式
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        // 去掉 "Bearer " 前缀后解析令牌；解析失败会抛出未授权异常
        AuthUser user = jwtService.parseToken(header.substring(7).trim());
        // 将当前用户放入线程上下文，供控制器/服务层读取
        AuthContext.set(user);
        LoggingContext.setUser(user.id(), user.role());

        // 仅当目标为控制器方法时才做角色校验
        if (handler instanceof HandlerMethod method) {
            // 优先取方法级 @RequireRole，取不到再回退到类级注解
            RequireRole required = method.getMethodAnnotation(RequireRole.class);
            if (required == null) {
                required = method.getBeanType().getAnnotation(RequireRole.class);
            }
            // 若声明了所需角色且当前用户角色不匹配，则拒绝访问
            if (required != null && Arrays.stream(required.value()).noneMatch(user.role()::equals)) {
                throw BusinessException.forbidden("当前账号无权执行此操作");
            }
        }
        return true;
    }

    /**
     * 请求完成后清理线程上下文，避免 ThreadLocal 泄漏。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
