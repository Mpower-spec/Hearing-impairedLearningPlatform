package com.hearing.platform.lesson;

import java.time.LocalDateTime;

public record LessonDto(
        Long id,
        String title,
        String pdfUrl,
        Integer pageCount,
        String status,
        LocalDateTime createdAt
) {
    public static LessonDto from(Lesson lesson, String pdfUrl) {
        return new LessonDto(lesson.getId(), lesson.getTitle(), pdfUrl,
                lesson.getPageCount(), lesson.getStatus(), lesson.getCreatedAt());
    }
}