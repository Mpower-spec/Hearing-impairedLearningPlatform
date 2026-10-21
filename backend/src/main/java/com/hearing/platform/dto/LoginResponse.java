package com.hearing.platform.dto;

public record LoginResponse(String token, UserDto user) {
}