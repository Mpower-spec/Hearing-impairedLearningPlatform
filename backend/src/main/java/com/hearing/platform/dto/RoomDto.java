package com.hearing.platform.dto;

import java.time.LocalDateTime; // 时间戳类型

/**
 * 课堂房间响应 DTO。
 * 聚合房间与其关联课件的展示信息。
 * @param id 房间 ID
 * @param lessonId 关联课件 ID
 * @param lessonTitle 课件标题
 * @param roomCode 房间邀请码
 * @param currentPage 当前页码
 * @param status 房间状态
 * @param pdfUrl 课件 PDF 地址
 * @param pageCount 课件页数
 * @param createdAt 创建时间
 * @param endedAt 结束时间
 */
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
