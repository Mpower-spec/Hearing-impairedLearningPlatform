package com.hearing.platform.dto;

/**
 * 登录响应 DTO。
 * 返回签发的 JWT 及登录用户信息。
 * @param token JWT 访问令牌
 * @param user 登录用户资料
 */
public record LoginResponse(String token, UserDto user) {
}
