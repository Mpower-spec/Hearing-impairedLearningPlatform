package com.hearing.platform.security;

/**
 * 角色常量定义。
 * 集中管理字符串角色值，避免各处硬编码，供 @RequireRole 与业务判断使用。
 */
public final class Roles {
    // 教师角色
    public static final String TEACHER = "TEACHER";
    // 学生角色
    public static final String STUDENT = "STUDENT";

    // 私有构造，禁止实例化（纯常量类）
    private Roles() {
    }
}
