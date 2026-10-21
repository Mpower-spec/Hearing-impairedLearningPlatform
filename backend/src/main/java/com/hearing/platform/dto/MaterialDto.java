package com.hearing.platform.dto;

import com.hearing.platform.entity.Material;

import java.time.LocalDateTime;

public record MaterialDto(Long id, Long lessonId, String type, String content, String source,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static MaterialDto from(Material material) {
        return new MaterialDto(material.getId(), material.getLessonId(), material.getType(),
                material.getContent(), material.getSource(), material.getCreatedAt(), material.getUpdatedAt());
    }
}