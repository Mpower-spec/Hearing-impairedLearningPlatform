package com.hearing.platform.room;

import java.time.LocalDateTime;

public record DanmakuDto(Long id, String content, String userName, LocalDateTime createdAt) {
}