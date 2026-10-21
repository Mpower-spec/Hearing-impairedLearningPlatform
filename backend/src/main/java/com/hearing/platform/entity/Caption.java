package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 字段映射注解
import com.baomidou.mybatisplus.annotation.TableId; // 主键映射注解
import com.baomidou.mybatisplus.annotation.TableName; // 表名映射注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 字幕实体。
 * 对应数据库表 caption。
 * 保存实时课堂中产生的语音字幕记录。
 */
@TableName("caption") // 映射到 caption 表
public class Caption {
    @TableId(type = IdType.AUTO) // 主键，数据库自增
    private Long id; // 字幕 ID
    @TableField("room_id") // 对应列 room_id
    private Long roomId; // 所属课堂房间 ID
    private String content; // 字幕文本内容
    @TableField("created_at") // 对应列 created_at
    private LocalDateTime createdAt; // 生成时间

    public Long getId() { return id; } // 读取字幕 ID
    public void setId(Long id) { this.id = id; } // 写入字幕 ID
    public Long getRoomId() { return roomId; } // 读取房间 ID
    public void setRoomId(Long roomId) { this.roomId = roomId; } // 写入房间 ID
    public String getContent() { return content; } // 读取字幕内容
    public void setContent(String content) { this.content = content; } // 写入字幕内容
    public LocalDateTime getCreatedAt() { return createdAt; } // 读取生成时间
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // 写入生成时间
}
