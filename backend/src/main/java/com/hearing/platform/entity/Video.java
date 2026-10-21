package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("video")
public class Video {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String title;
    @TableField("video_path")
    private String videoPath;
    @TableField("audio_path")
    private String audioPath;
    @TableField("vtt_path")
    private String vttPath;
    private String transcript;
    private String note;
    private String status;
    @TableField("transcript_source")
    private String transcriptSource;
    @TableField("note_source")
    private String noteSource;
    @TableField("timing_mode")
    private String timingMode;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public String getVttPath() { return vttPath; }
    public void setVttPath(String vttPath) { this.vttPath = vttPath; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTranscriptSource() { return transcriptSource; }
    public void setTranscriptSource(String transcriptSource) { this.transcriptSource = transcriptSource; }
    public String getNoteSource() { return noteSource; }
    public void setNoteSource(String noteSource) { this.noteSource = noteSource; }
    public String getTimingMode() { return timingMode; }
    public void setTimingMode(String timingMode) { this.timingMode = timingMode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}