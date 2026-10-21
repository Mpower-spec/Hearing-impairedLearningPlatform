package com.hearing.platform.dto;

import com.hearing.platform.entity.AppUser; // 用户实体
import com.hearing.platform.security.AuthUser; // 认证上下文用户

/**
 * 用户响应 DTO。
 * 对外输出用户基本信息，不含密码。
 * @param id 用户 ID
 * @param username 用户名
 * @param name 展示名
 * @param role 角色
 */
public record UserDto(Long id, String username, String name, String role) {
    /**
     * 由数据库用户实体转换。
     * @param user 用户实体
     * @return 用户 DTO
     */
    public static UserDto from(AppUser user) {
        return new UserDto(user.getId(), user.getUsername(), user.getName(), user.getRole());
    }

    /**
     * 由认证上下文用户转换。
     * @param user 认证用户
     * @return 用户 DTO
     */
    public static UserDto from(AuthUser user) {
        return new UserDto(user.id(), user.username(), user.name(), user.role());
    }
}
