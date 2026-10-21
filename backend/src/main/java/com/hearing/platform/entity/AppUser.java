package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 字段映射注解
import com.baomidou.mybatisplus.annotation.TableId; // 主键映射注解
import com.baomidou.mybatisplus.annotation.TableName; // 表名映射注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 用户实体。
 * 对应数据库表 app_user。
 * 承载登录账号、密码哈希、角色等信息。
 */
@TableName("app_user") // 映射到 app_user 表
public class AppUser {
    @TableId(type = IdType.AUTO) // 主键，数据库自增
    private Long id; // 用户 ID
    private String username; // 登录用户名
    @TableField("password_hash") // 对应列 password_hash
    private String passwordHash; // BCrypt 加密后的密码
    private String name; // 展示名称
    private String role; // 角色：teacher/student/admin
    @TableField("created_at") // 对应列 created_at
    private LocalDateTime createdAt; // 创建时间

    public Long getId() { return id; } // 读取用户 ID
    public void setId(Long id) { this.id = id; } // 写入用户 ID
    public String getUsername() { return username; } // 读取用户名
    public void setUsername(String username) { this.username = username; } // 写入用户名
    public String getPasswordHash() { return passwordHash; } // 读取密码哈希
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; } // 写入密码哈希
    public String getName() { return name; } // 读取展示名称
    public void setName(String name) { this.name = name; } // 写入展示名称
    public String getRole() { return role; } // 读取角色
    public void setRole(String role) { this.role = role; } // 写入角色
    public LocalDateTime getCreatedAt() { return createdAt; } // 读取创建时间
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // 写入创建时间
}
