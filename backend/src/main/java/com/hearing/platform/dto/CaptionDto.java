package com.hearing.platform.dto;

import com.hearing.platform.entity.Caption; // 字幕实体

import java.time.LocalDateTime; // 时间戳类型

/**
 * 字幕响应 DTO。
 * 面向前端输出字幕的展示字段。
 * @param id 字幕 ID
 * @param content 字幕内容
 * @param createdAt 生成时间
 */
public record CaptionDto(Long id, String content, LocalDateTime createdAt) {
    /**
     * 由实体转换为 DTO。
     * @param caption 字幕实体
     * @return 字幕 DTO
     */
    public static CaptionDto from(Caption caption) {
        return new CaptionDto(caption.getId(), caption.getContent(), caption.getCreatedAt());
    }
}
