package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;
import com.hearing.platform.dto.MaterialDto;
import com.hearing.platform.dto.VideoDto;
import com.hearing.platform.security.AuthContext;
import com.hearing.platform.security.RequireRole;
import com.hearing.platform.security.Roles;
import com.hearing.platform.service.AiService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 能力接口控制器。
 * 提供基于大模型（LLM）与语音识别（ASR）的备课资料生成、课堂学习资料生成、
 * 视频字幕转写及学习笔记生成等接口，统一前缀为 /api/ai。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 生成备课资料。
     * 依据课程 PPT 文本通过 AI 生成备课资料并返回。
     *
     * @param lessonId 课程 ID
     * @return 生成的备课资料
     */
    @PostMapping("/prepare/{lessonId}")
    @RequireRole(Roles.TEACHER)
    public ApiResponse<MaterialDto> prepare(@PathVariable Long lessonId) {
        return ApiResponse.ok("备课资料已生成", aiService.prepare(lessonId, AuthContext.get()));
    }

    /**
     * 生成备课资料。
     * 依据课程字幕（教师可用 PPT）通过 AI 生成课堂学习资料并返回。
     * 教师可直接生成；学生需在课堂结束后才能生成。
     *
     * @param lessonId 课程 ID
     * @return 生成的课堂学习资料
     */
    @PostMapping("/material/{lessonId}")
    public ApiResponse<MaterialDto> material(@PathVariable Long lessonId) {
        return ApiResponse.ok("课堂学习资料已生成", aiService.classMaterial(lessonId, AuthContext.get()));
    }

    /**
     * 视频字幕转写。
     * 仅学生可调用，提取视频音频并通过 ASR 转写生成字幕（VTT），结果写回视频记录。
     *
     * @param videoId 视频 ID
     * @return 更新后的视频信息
     */
    @PostMapping("/transcribe/{videoId}")
    @RequireRole(Roles.STUDENT)
    public ApiResponse<VideoDto> transcribe(@PathVariable Long videoId) {
        return ApiResponse.ok("字幕已生成", aiService.transcribe(videoId, AuthContext.get()));
    }

    /**
     * 生成学习笔记。
     * 仅学生可调用，需先完成字幕转写，再依据视频字幕通过 AI 生成学习笔记。
     *
     * @param videoId 视频 ID
     * @return 更新后的视频信息（含学习笔记）
     */
    @PostMapping("/note/{videoId}")
    @RequireRole(Roles.STUDENT)
    public ApiResponse<VideoDto> note(@PathVariable Long videoId) {
        return ApiResponse.ok("学习笔记已生成", aiService.note(videoId, AuthContext.get()));
    }
}