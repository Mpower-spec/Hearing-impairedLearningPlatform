package com.hearing.platform;

import org.mybatis.spring.annotation.MapperScan; // 扫描 MyBatis Mapper 接口
import org.springframework.boot.SpringApplication; // Spring Boot 启动器
import org.springframework.boot.autoconfigure.SpringBootApplication; // 自动配置 + 组件扫描 + 配置类

/**
 * 应用主启动类。
 * 听障学习平台后端的入口。
 */
@SpringBootApplication // 启用自动配置并扫描本包及子包
@MapperScan("com.hearing.platform.dao") // 指定 DAO 接口包，自动注册为 Mapper Bean
public class HearingPlatformApplication {

    /**
     * 程序入口，启动 Spring 容器。
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HearingPlatformApplication.class, args); // 引导应用运行
    }
}
