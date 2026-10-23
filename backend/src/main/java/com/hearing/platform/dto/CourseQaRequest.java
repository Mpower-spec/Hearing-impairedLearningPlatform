package com.hearing.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseQaRequest(
        @NotNull(message = "缺少课堂信息")
        Long roomId,
        @NotBlank(message = "请输入问题")
        @Size(max = 500, message = "问题不能超过500个字符")
        String question
) {
}
