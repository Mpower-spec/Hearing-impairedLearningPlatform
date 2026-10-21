package com.hearing.platform.dto;

import java.time.LocalDateTime; // 时间戳类型

/**
 * 弹幕响应 DTO。
 * 面向前端输出弹幕消息，附带发送者名称。
 * @param id 弹幕 ID
 * @param content 弹幕内容
 * @param userName 发送者展示名
 * @param createdAt 发送时间
 */
public record DanmakuDto(Long id, String content, String userName, LocalDateTime createdAt) {
}
