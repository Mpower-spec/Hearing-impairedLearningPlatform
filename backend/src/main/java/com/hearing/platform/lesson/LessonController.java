package com.hearing.platform.lesson;

import com.hearing.platform.auth.AuthContext;
import com.hearing.platform.auth.RequireRole;
import com.hearing.platform.auth.Roles;
import com.hearing.platform.common.ApiResponse;
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
    private final com.hearing.platform.storage.FileStorageService storageService;

    public LessonController(LessonService lessonService, com.hearing.platform.storage.FileStorageService storageService) {
        this.lessonService = lessonService;
        this.storageService = storageService;
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
        Lesson lesson = lessonService.requireOwned(id, AuthContext.get());
        return ApiResponse.ok(LessonDto.from(lesson, storageService.url(lesson.getPdfPath())));
    }
}