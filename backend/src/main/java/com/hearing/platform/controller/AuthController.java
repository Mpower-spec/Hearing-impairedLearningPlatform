package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;   // 统一响应封装体
import com.hearing.platform.dto.LoginRequest;      // 登录请求参数对象
import com.hearing.platform.dto.LoginResponse;     // 登录成功返回体（令牌 + 用户）
import com.hearing.platform.dto.UserDto;           // 用户信息展示对象
import com.hearing.platform.security.AuthContext;  // 存放当前登录用户的上下文
import com.hearing.platform.service.AuthService;   // 认证业务服务

import jakarta.validation.Valid;                   // 入参校验注解
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口控制器。
 * 负责用户登录与获取当前登录用户信息，统一前缀为 /api/auth。
 * 其中 /api/auth/login 在 WebConfig 中被放行，无需携带令牌。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 认证业务服务，由 Spring 通过构造器注入
    private final AuthService authService;

    // 构造器注入 AuthService
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求体（用户名、密码），@Valid 触发字段级校验
     * @return 包含 JWT 令牌与用户信息的响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // 委托服务层校验凭据并签发令牌，成功消息使用默认值
        return ApiResponse.ok(authService.login(request));
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户的信息
     */
    @GetMapping("/me")
    public ApiResponse<UserDto> me() {
        // 从上下文取出当前登录用户，交给服务层转换为 DTO
        return ApiResponse.ok(authService.current(AuthContext.get()));
    }
}
