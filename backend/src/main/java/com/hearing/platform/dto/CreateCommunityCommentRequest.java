package com.hearing.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityCommentRequest(
        @NotBlank(message = "请输入评论内容")
        @Size(max = 2000, message = "评论内容不能超过2000个字符")
        String content
) {
}
