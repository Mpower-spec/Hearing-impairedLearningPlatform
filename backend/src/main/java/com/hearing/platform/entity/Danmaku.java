package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 字段映射注解
import com.baomidou.mybatisplus.annotation.TableId; // 主键映射注解
import com.baomidou.mybatisplus.annotation.TableName; // 表名映射注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 弹幕实体。
 * 对应数据库表 danmaku。
 * 保存课堂房间内用户发送的弹幕消息。
 */
@TableName("danmaku") // 映射到 danmaku 表
public class Danmaku {
    @TableId(type = IdType.AUTO) // 主键，数据库自增
    private Long id; // 弹幕 ID
    @TableField("room_id") // 对应列 room_id
    private Long roomId; // 所属房间 ID
    @TableField("user_id") // 对应列 user_id
    private Long userId; // 发送者用户 ID
    private String content; // 弹幕文本内容
    @TableField("created_at") // 对应列 created_at
    private LocalDateTime createdAt; // 发送时间

    public Long getId() { return id; } // 读取弹幕 ID
    public void setId(Long id) { this.id = id; } // 写入弹幕 ID
    public Long getRoomId() { return roomId; } // 读取房间 ID
    public void setRoomId(Long roomId) { this.roomId = roomId; } // 写入房间 ID
    public Long getUserId() { return userId; } // 读取发送者 ID
    public void setUserId(Long userId) { this.userId = userId; } // 写入发送者 ID
    public String getContent() { return content; } // 读取弹幕内容
    public void setContent(String content) { this.content = content; } // 写入弹幕内容
    public LocalDateTime getCreatedAt() { return createdAt; } // 读取发送时间
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // 写入发送时间
}
