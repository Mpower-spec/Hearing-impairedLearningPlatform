package com.hearing.platform.security;

/**
 * 已认证用户的轻量表示。
 * 从 JWT 载荷还原，仅携带鉴权与业务判断所需的关键字段。
 *
 * @param id       用户 ID
 * @param username 用户名
 * @param name     昵称
 * @param role     角色（TEACHER / STUDENT）
 */
public record AuthUser(Long id, String username, String name, String role) {
    // 是否为教师角色
    public boolean isTeacher() { return "TEACHER".equals(role); }
    // 是否为学生角色
    public boolean isStudent() { return "STUDENT".equals(role); }
}
