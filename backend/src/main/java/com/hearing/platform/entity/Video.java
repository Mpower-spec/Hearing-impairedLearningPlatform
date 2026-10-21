package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 字段映射注解
import com.baomidou.mybatisplus.annotation.TableId; // 主键映射注解
import com.baomidou.mybatisplus.annotation.TableName; // 表名映射注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 视频实体。
 * 对应数据库表 video。
 * 记录用户上传视频及其音频、字幕、转写、笔记等处理产物。
 */
@TableName("video") // 映射到 video 表
public class Video {
    @TableId(type = IdType.AUTO) // 主键，数据库自增
    private Long id; // 视频 ID
    @TableField("user_id") // 对应列 user_id
    private Long userId; // 上传者用户 ID
    private String title; // 视频标题
    @TableField("video_path") // 对应列 video_path
    private String videoPath; // 视频文件存储路径
    @TableField("audio_path") // 对应列 audio_path
    private String audioPath; // 抽取出的音频路径
    @TableField("vtt_path") // 对应列 vtt_path
    private String vttPath; // 生成的 VTT 字幕路径
    private String transcript; // 语音转写的文本内容
    private String note; // AI 生成的学习笔记
    private String status; // 处理状态
    @TableField("transcript_source") // 对应列 transcript_source
    private String transcriptSource; // 转写来源（real/mock）
    @TableField("note_source") // 对应列 note_source
    private String noteSource; // 笔记来源（real/mock）
    @TableField("timing_mode") // 对应列 timing_mode
    private String timingMode; // 字幕时间轴模式
    @TableField("created_at") // 对应列 created_at
    private LocalDateTime createdAt; // 创建时间
    @TableField("updated_at") // 对应列 updated_at
    private LocalDateTime updatedAt; // 更新时间

    public Long getId() { return id; } // 读取视频 ID
    public void setId(Long id) { this.id = id; } // 写入视频 ID
    public Long getUserId() { return userId; } // 读取上传者 ID
    public void setUserId(Long userId) { this.userId = userId; } // 写入上传者 ID
    public String getTitle() { return title; } // 读取标题
    public void setTitle(String title) { this.title = title; } // 写入标题
    public String getVideoPath() { return videoPath; } // 读取视频路径
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; } // 写入视频路径
    public String getAudioPath() { return audioPath; } // 读取音频路径
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; } // 写入音频路径
    public String getVttPath() { return vttPath; } // 读取字幕路径
    public void setVttPath(String vttPath) { this.vttPath = vttPath; } // 写入字幕路径
    public String getTranscript() { return transcript; } // 读取转写文本
    public void setTranscript(String transcript) { this.transcript = transcript; } // 写入转写文本
    public String getNote() { return note; } // 读取笔记
    public void setNote(String note) { this.note = note; } // 写入笔记
    public String getStatus() { return status; } // 读取状态
    public void setStatus(String status) { this.status = status; } // 写入状态
    public String getTranscriptSource() { return transcriptSource; } // 读取转写来源
    public void setTranscriptSource(String transcriptSource) { this.transcriptSource = transcriptSource; } // 写入转写来源
    public String getNoteSource() { return noteSource; } // 读取笔记来源
    public void setNoteSource(String noteSource) { this.noteSource = noteSource; } // 写入笔记来源
    public String getTimingMode() { return timingMode; } // 读取时间轴模式
    public void setTimingMode(String timingMode) { this.timingMode = timingMode; } // 写入时间轴模式
    public LocalDateTime getCreatedAt() { return createdAt; } // 读取创建时间
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // 写入创建时间
    public LocalDateTime getUpdatedAt() { return updatedAt; } // 读取更新时间
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; } // 写入更新时间
}
