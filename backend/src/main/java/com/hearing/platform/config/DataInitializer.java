package com.hearing.platform.config;

import com.hearing.platform.dao.UserMapper; // 用户表数据访问
import com.hearing.platform.entity.AppUser; // 用户实体
import com.hearing.platform.security.Roles; // 角色常量

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // 类型安全条件构造器
import org.springframework.boot.CommandLineRunner; // 启动后执行回调
import org.springframework.security.crypto.password.PasswordEncoder; // 密码编码器
import org.springframework.stereotype.Component; // Bean 组件标记

import java.time.LocalDateTime; // 时间戳

/**
 * 数据初始化器。
 * 应用启动后插入演示账号，便于快速登录测试。
 * 幂等：已存在同名用户则跳过。
 */
@Component // 注册为 Spring Bean
public class DataInitializer implements CommandLineRunner { // 实现启动回调接口

    private final UserMapper userMapper; // 用户数据访问
    private final PasswordEncoder passwordEncoder; // 密码加密器

    /**
     * 构造器注入依赖。
     * @param userMapper 用户数据访问
     * @param passwordEncoder 密码加密器
     */
    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 启动时执行：写入教师与学生演示账号。
     * 密码统一为 123456。
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        seed("teacher", "123456", "演示教师", Roles.TEACHER); // 种子教师账号
        seed("student", "123456", "演示学生", Roles.STUDENT); // 种子学生账号
    }

    /**
     * 插入单个用户，若已存在则跳过。
     * @param username 用户名
     * @param password 明文密码（内部会加密存储）
     * @param name 显示名称
     * @param role 角色
     */
    private void seed(String username, String password, String name, String role) {
        // 先按用户名统计，判断是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, username));
        if (count != null && count > 0) {
            return; // 已存在则幂等跳过
        }
        AppUser user = new AppUser(); // 新建用户实体
        user.setUsername(username); // 设置用户名
        user.setPasswordHash(passwordEncoder.encode(password)); // 密码 BCrypt 加密后存储
        user.setName(name); // 设置显示名称
        user.setRole(role); // 设置角色
        user.setCreatedAt(LocalDateTime.now()); // 记录创建时间
        userMapper.insert(user); // 插入数据库
    }
}
