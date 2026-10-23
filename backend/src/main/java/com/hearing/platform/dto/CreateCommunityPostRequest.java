package com.hearing.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityPostRequest(
        @NotBlank(message = "请选择讨论分类")
        @Size(max = 30, message = "讨论分类过长")
        String category,
        @NotBlank(message = "请输入帖子标题")
        @Size(max = 120, message = "帖子标题不能超过120个字符")
        String title,
        @NotBlank(message = "请输入帖子内容")
        @Size(max = 5000, message = "帖子内容不能超过5000个字符")
        String content
) {
}
