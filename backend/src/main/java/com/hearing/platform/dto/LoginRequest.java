package com.hearing.platform.dto;

import jakarta.validation.constraints.NotBlank; // 非空白校验注解

/**
 * 登录请求 DTO。
 * 前端提交用户名与密码进行登录。
 * @param username 用户名（必填）
 * @param password 密码（必填）
 */
public record LoginRequest(@NotBlank String username, @NotBlank String password) { // 两者均不能为空白
}
