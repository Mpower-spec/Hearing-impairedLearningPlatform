package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 字段映射注解
import com.baomidou.mybatisplus.annotation.TableId; // 主键映射注解
import com.baomidou.mybatisplus.annotation.TableName; // 表名映射注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 房间成员实体。
 * 对应数据库表 room_member。
 * 记录用户与课堂房间的加入关系。
 */
@TableName("room_member") // 映射到 room_member 表
public class RoomMember {
    @TableId(type = IdType.AUTO) // 主键，数据库自增
    private Long id; // 记录 ID
    @TableField("room_id") // 对应列 room_id
    private Long roomId; // 房间 ID
    @TableField("user_id") // 对应列 user_id
    private Long userId; // 成员用户 ID
    @TableField("joined_at") // 对应列 joined_at
    private LocalDateTime joinedAt; // 加入时间

    public Long getId() { return id; } // 读取记录 ID
    public void setId(Long id) { this.id = id; } // 写入记录 ID
    public Long getRoomId() { return roomId; } // 读取房间 ID
    public void setRoomId(Long roomId) { this.roomId = roomId; } // 写入房间 ID
    public Long getUserId() { return userId; } // 读取成员 ID
    public void setUserId(Long userId) { this.userId = userId; } // 写入成员 ID
    public LocalDateTime getJoinedAt() { return joinedAt; } // 读取加入时间
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; } // 写入加入时间
}
