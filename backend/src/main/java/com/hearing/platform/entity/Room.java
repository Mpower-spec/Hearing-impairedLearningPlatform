package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 字段映射注解
import com.baomidou.mybatisplus.annotation.TableId; // 主键映射注解
import com.baomidou.mybatisplus.annotation.TableName; // 表名映射注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 课堂房间实体。
 * 对应数据库表 room。
 * 表示一次基于课件的实时授课会话。
 */
@TableName("room") // 映射到 room 表
public class Room {
    @TableId(type = IdType.AUTO) // 主键，数据库自增
    private Long id; // 房间 ID
    @TableField("lesson_id") // 对应列 lesson_id
    private Long lessonId; // 关联课件 ID
    @TableField("room_code") // 对应列 room_code
    private String roomCode; // 房间邀请码
    @TableField("current_page") // 对应列 current_page
    private Integer currentPage; // 当前翻页页码
    private String status; // 房间状态（进行中/已结束）
    @TableField("created_at") // 对应列 created_at
    private LocalDateTime createdAt; // 创建时间
    @TableField("ended_at") // 对应列 ended_at
    private LocalDateTime endedAt; // 结束时间

    public Long getId() { return id; } // 读取房间 ID
    public void setId(Long id) { this.id = id; } // 写入房间 ID
    public Long getLessonId() { return lessonId; } // 读取课件 ID
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; } // 写入课件 ID
    public String getRoomCode() { return roomCode; } // 读取邀请码
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; } // 写入邀请码
    public Integer getCurrentPage() { return currentPage; } // 读取当前页码
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; } // 写入当前页码
    public String getStatus() { return status; } // 读取状态
    public void setStatus(String status) { this.status = status; } // 写入状态
    public LocalDateTime getCreatedAt() { return createdAt; } // 读取创建时间
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // 写入创建时间
    public LocalDateTime getEndedAt() { return endedAt; } // 读取结束时间
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; } // 写入结束时间
}
