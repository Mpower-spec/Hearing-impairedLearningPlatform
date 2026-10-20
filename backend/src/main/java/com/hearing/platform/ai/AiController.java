package com.hearing.platform.ai;

import com.hearing.platform.auth.AuthContext;
import com.hearing.platform.auth.RequireRole;
import com.hearing.platform.auth.Roles;
import com.hearing.platform.common.ApiResponse;
import com.hearing.platform.material.MaterialDto;
import com.hearing.platform.video.VideoDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/prepare/{lessonId}")
    @RequireRole(Roles.TEACHER)
    public ApiResponse<MaterialDto> prepare(@PathVariable Long lessonId) {
        return ApiResponse.ok("备课资料已生成", aiService.prepare(lessonId, AuthContext.get()));
    }

    @PostMapping("/material/{lessonId}")
    public ApiResponse<MaterialDto> material(@PathVariable Long lessonId) {
        return ApiResponse.ok("课堂学习资料已生成", aiService.classMaterial(lessonId, AuthContext.get()));
    }

    @PostMapping("/transcribe/{videoId}")
    @RequireRole(Roles.STUDENT)
    public ApiResponse<VideoDto> transcribe(@PathVariable Long videoId) {
        return ApiResponse.ok("字幕已生成", aiService.transcribe(videoId, AuthContext.get()));
    }

    @PostMapping("/note/{videoId}")
    @RequireRole(Roles.STUDENT)
    public ApiResponse<VideoDto> note(@PathVariable Long videoId) {
        return ApiResponse.ok("学习笔记已生成", aiService.note(videoId, AuthContext.get()));
    }
}