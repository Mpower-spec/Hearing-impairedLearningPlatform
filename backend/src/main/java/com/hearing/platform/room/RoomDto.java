package com.hearing.platform.room;

import java.time.LocalDateTime;

public record RoomDto(
        Long id,
        Long lessonId,
        String lessonTitle,
        String roomCode,
        Integer currentPage,
        String status,
        String pdfUrl,
        Integer pageCount,
        LocalDateTime createdAt,
        LocalDateTime endedAt
) {
}