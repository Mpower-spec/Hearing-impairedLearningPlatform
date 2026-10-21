package com.hearing.platform.dto;

import com.hearing.platform.entity.Video;

import java.time.LocalDateTime;

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
    public static VideoDto from(Video video, String videoUrl, String vttUrl) {
        return new VideoDto(video.getId(), video.getTitle(), videoUrl, vttUrl, video.getTranscript(),
                video.getNote(), video.getStatus(), video.getTranscriptSource(), video.getNoteSource(),
                video.getTimingMode(), video.getCreatedAt(), video.getUpdatedAt());
    }
}