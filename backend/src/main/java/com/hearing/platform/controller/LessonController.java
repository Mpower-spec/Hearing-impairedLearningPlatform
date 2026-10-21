package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;
import com.hearing.platform.dto.LessonDto;
import com.hearing.platform.security.AuthContext;
import com.hearing.platform.security.RequireRole;
import com.hearing.platform.security.Roles;
import com.hearing.platform.service.LessonService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequireRole(Roles.TEACHER)
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public ApiResponse<List<LessonDto>> list() {
        return ApiResponse.ok(lessonService.listByTeacher(AuthContext.get()));
    }

    @PostMapping
    public ApiResponse<LessonDto> upload(@RequestParam String title,
                                         @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("课程已创建", lessonService.upload(AuthContext.get(), title, file));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        lessonService.deleteOwned(id, AuthContext.get());
        return ApiResponse.ok("课程已删除", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<LessonDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(lessonService.detail(id, AuthContext.get()));
    }
}