package com.hearing.platform.room;

import java.time.LocalDateTime;

public record CaptionDto(Long id, String content, LocalDateTime createdAt) {
    public static CaptionDto from(Caption caption) {
        return new CaptionDto(caption.getId(), caption.getContent(), caption.getCreatedAt());
    }
}