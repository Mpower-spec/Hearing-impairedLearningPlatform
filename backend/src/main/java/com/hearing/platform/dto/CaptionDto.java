package com.hearing.platform.dto;

import com.hearing.platform.entity.Caption;

import java.time.LocalDateTime;

public record CaptionDto(Long id, String content, LocalDateTime createdAt) {
    public static CaptionDto from(Caption caption) {
        return new CaptionDto(caption.getId(), caption.getContent(), caption.getCreatedAt());
    }
}