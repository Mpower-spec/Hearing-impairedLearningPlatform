package com.hearing.platform.dto;

import java.util.List;

public record CommunityPostDetailDto(
        CommunityPostDto post,
        List<CommunityCommentDto> comments
) {
}
