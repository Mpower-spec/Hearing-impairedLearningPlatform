package com.hearing.platform.config;

import com.hearing.platform.dao.UserMapper;
import com.hearing.platform.entity.AppUser;
import com.hearing.platform.security.Roles;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seed("teacher", "123456", "演示教师", Roles.TEACHER);
        seed("student", "123456", "演示学生", Roles.STUDENT);
    }

    private void seed(String username, String password, String name, String role) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<AppUser>().eq(AppUser::getUsername, username));
        if (count != null && count > 0) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }
}