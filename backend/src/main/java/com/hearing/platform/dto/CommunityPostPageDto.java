package com.hearing.platform.dto;

import java.util.List;

public record CommunityPostPageDto(
        List<CommunityPostDto> items,
        long total,
        long page,
        long size,
        long totalPages
) {
}
