package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;
import com.hearing.platform.dto.CourseQaRequest;
import com.hearing.platform.dto.CourseQaResponse;
import com.hearing.platform.security.AuthContext;
import com.hearing.platform.security.RequireRole;
import com.hearing.platform.security.Roles;
import com.hearing.platform.service.CourseQaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-qa")
@RequireRole(Roles.STUDENT)
public class CourseQaController {

    private final CourseQaService courseQaService;

    public CourseQaController(CourseQaService courseQaService) {
        this.courseQaService = courseQaService;
    }

    @PostMapping("/ask")
    public ApiResponse<CourseQaResponse> ask(@Valid @RequestBody CourseQaRequest request) {
        return ApiResponse.ok(courseQaService.ask(request, AuthContext.get()));
    }
}
