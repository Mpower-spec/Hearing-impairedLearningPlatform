package com.hearing.platform.dto;

import java.time.LocalDateTime;

public record DanmakuDto(Long id, String content, String userName, LocalDateTime createdAt) {
}