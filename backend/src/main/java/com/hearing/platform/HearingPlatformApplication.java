package com.hearing.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hearing.platform")
public class HearingPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(HearingPlatformApplication.class, args);
    }
}