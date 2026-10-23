package com.hearing.platform.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties; // 启用配置属性绑定
import org.springframework.context.annotation.Bean; // 声明 Bean
import org.springframework.context.annotation.Configuration; // 配置类标记
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // BCrypt 密码编码器
import org.springframework.security.crypto.password.PasswordEncoder; // 密码编码器接口

/**
 * 应用基础配置。
 * 注册全局密码编码器，并启用自定义配置属性类。
 */
@Configuration // 声明为 Spring 配置类
@EnableConfigurationProperties({AppProperties.class, AiProperties.class}) // 绑定 app.* 与 ai.* 配置
public class AppConfig {

    /**
     * 密码编码器 Bean。
     * 使用 BCrypt 算法，供注册时加密、登录时校验。
     * @return BCrypt 编码器实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
