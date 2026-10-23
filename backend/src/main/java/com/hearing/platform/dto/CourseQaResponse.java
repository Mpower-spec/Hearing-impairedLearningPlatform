package com.hearing.platform.dto;

import java.util.List;

public record CourseQaResponse(
        String answer,
        String source,
        List<CourseQaSourceDto> sources
) {
}
