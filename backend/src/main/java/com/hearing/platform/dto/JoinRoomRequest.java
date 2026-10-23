package com.hearing.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 学生通过房间码加入课堂的请求。
 * 加入动作会立即写入 room_member，避免仅依赖 WebSocket 时因快速离开而丢失记录。
 */
public record JoinRoomRequest(
        @NotBlank(message = "请输入房间码")
        @Pattern(regexp = "\\d{6}", message = "房间码必须是 6 位数字")
        String roomCode
) {
}