package com.hearing.platform.dto;

import com.hearing.platform.entity.Lesson; // 课件实体

import java.time.LocalDateTime; // 时间戳类型

/**
 * 课件响应 DTO。
 * 面向前端输出课件信息，pdfUrl 为拼接好的可访问地址。
 * @param id 课件 ID
 * @param title 课件标题
 * @param pdfUrl PDF 访问地址
 * @param pageCount 页数
 * @param status 处理状态
 * @param createdAt 创建时间
 */
public record LessonDto(
        Long id,
        String title,
        String pdfUrl,
        Integer pageCount,
        String status,
        LocalDateTime createdAt
) {
    /**
     * 由实体与外部计算的 pdfUrl 组装 DTO。
     * @param lesson 课件实体
     * @param pdfUrl 拼接好的 PDF 访问地址
     * @return 课件 DTO
     */
    public static LessonDto from(Lesson lesson, String pdfUrl) {
        return new LessonDto(lesson.getId(), lesson.getTitle(), pdfUrl,
                lesson.getPageCount(), lesson.getStatus(), lesson.getCreatedAt());
    }
}
