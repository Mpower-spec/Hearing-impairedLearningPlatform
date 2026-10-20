package com.hearing.platform.auth;

public record AuthUser(Long id, String username, String name, String role) {
    public boolean isTeacher() { return "TEACHER".equals(role); }
    public boolean isStudent() { return "STUDENT".equals(role); }
}