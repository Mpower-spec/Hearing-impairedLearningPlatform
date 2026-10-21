package com.hearing.platform.dto;

import com.hearing.platform.entity.Video; // 视频实体

import java.time.LocalDateTime; // 时间戳类型

/**
 * 视频响应 DTO。
 * 面向前端输出视频及其处理产物，videoUrl/vttUrl 为拼接好的可访问地址。
 * @param id 视频 ID
 * @param title 标题
 * @param videoUrl 视频访问地址
 * @param vttUrl 字幕访问地址
 * @param transcript 转写文本
 * @param note 学习笔记
 * @param status 处理状态
 * @param transcriptSource 转写来源
 * @param noteSource 笔记来源
 * @param timingMode 时间轴模式
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record VideoDto(
        Long id,
        String title,
        String videoUrl,
        String vttUrl,
        String transcript,
        String note,
        String status,
        String transcriptSource,
        String noteSource,
        String timingMode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 由实体与外部拼接的访问地址组装 DTO。
     * @param video 视频实体
     * @param videoUrl 视频访问地址
     * @param vttUrl 字幕访问地址
     * @return 视频 DTO
     */
    public static VideoDto from(Video video, String videoUrl, String vttUrl) {
        return new VideoDto(video.getId(), video.getTitle(), videoUrl, vttUrl, video.getTranscript(),
                video.getNote(), video.getStatus(), video.getTranscriptSource(), video.getNoteSource(),
                video.getTimingMode(), video.getCreatedAt(), video.getUpdatedAt());
    }
}
