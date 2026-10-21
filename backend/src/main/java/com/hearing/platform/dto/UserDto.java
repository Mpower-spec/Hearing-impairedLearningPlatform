package com.hearing.platform.dto;

import com.hearing.platform.entity.AppUser;
import com.hearing.platform.security.AuthUser;

public record UserDto(Long id, String username, String name, String role) {
    public static UserDto from(AppUser user) {
        return new UserDto(user.getId(), user.getUsername(), user.getName(), user.getRole());
    }

    public static UserDto from(AuthUser user) {
        return new UserDto(user.id(), user.username(), user.name(), user.role());
    }
}