package com.hearing.platform.user;

import com.hearing.platform.auth.AuthUser;

public record UserDto(Long id, String username, String name, String role) {
    public static UserDto from(AppUser user) {
        return new UserDto(user.getId(), user.getUsername(), user.getName(), user.getRole());
    }

    public static UserDto from(AuthUser user) {
        return new UserDto(user.id(), user.username(), user.name(), user.role());
    }
}