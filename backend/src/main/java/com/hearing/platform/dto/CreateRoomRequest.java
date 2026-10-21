package com.hearing.platform.dto;

import jakarta.validation.constraints.NotNull; // 非空校验注解

/**
 * 创建房间请求 DTO。
 * 前端发起建课请求时提交负载。
 * @param lessonId 所依据课件 ID（必填）
 */
public record CreateRoomRequest(@NotNull Long lessonId) { // lessonId 不允许为 null
}
