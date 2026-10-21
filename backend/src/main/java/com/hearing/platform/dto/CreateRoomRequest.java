package com.hearing.platform.dto;

import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(@NotNull Long lessonId) {
}