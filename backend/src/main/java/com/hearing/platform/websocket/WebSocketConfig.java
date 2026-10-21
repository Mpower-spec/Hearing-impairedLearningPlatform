package com.hearing.platform.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类。
 * 启用原生 WebSocket 支持，并将课堂处理器注册到 /ws/classroom 端点。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    // 课堂实时交互处理器，构造器注入
    private final ClassroomWebSocketHandler handler;

    // 构造器注入 ClassroomWebSocketHandler
    public WebSocketConfig(ClassroomWebSocketHandler handler) {
        this.handler = handler;
    }

    /**
     * 注册 WebSocket 处理器。
     * 将 handler 绑定到 /ws/classroom，并允许来自任意来源的连接（演示用途）。
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/classroom").setAllowedOriginPatterns("*");
    }
}
