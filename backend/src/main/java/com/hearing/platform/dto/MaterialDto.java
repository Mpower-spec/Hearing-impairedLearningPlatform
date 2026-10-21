package com.hearing.platform.dto;

import com.hearing.platform.entity.Material; // 学习资料实体

import java.time.LocalDateTime; // 时间戳类型

/**
 * 学习资料响应 DTO。
 * 面向前端输出 AI 生成的课件衍生内容。
 * @param id 资料 ID
 * @param lessonId 关联课件 ID
 * @param type 资料类型
 * @param content 资料正文
 * @param source 内容来源（real/mock）
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record MaterialDto(Long id, Long lessonId, String type, String content, String source,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
    /**
     * 由实体转换为 DTO。
     * @param material 学习资料实体
     * @return 学习资料 DTO
     */
    public static MaterialDto from(Material material) {
        return new MaterialDto(material.getId(), material.getLessonId(), material.getType(),
                material.getContent(), material.getSource(), material.getCreatedAt(), material.getUpdatedAt());
    }
}
