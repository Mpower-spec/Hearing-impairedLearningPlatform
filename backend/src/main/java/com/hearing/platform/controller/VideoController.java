package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;   // 统一响应封装体
import com.hearing.platform.dto.VideoDto;          // 视频信息展示对象
import com.hearing.platform.security.AuthContext;  // 当前登录用户上下文
import com.hearing.platform.security.RequireRole;  // 角色访问控制注解
import com.hearing.platform.security.Roles;        // 角色常量定义
import com.hearing.platform.service.VideoService;  // 视频业务服务

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile; // 上传文件对象

import java.util.List;

/**
 * 视频接口控制器。
 * 提供学生端的视频列表、上传、删除、详情接口，统一前缀 /api/videos。
 * 类级 @RequireRole 限制：本控制器所有接口仅学生角色可访问。
 * 字幕转写、笔记生成等 AI 能力在 AiController。
 */
@RestController
@RequestMapping("/api/videos")
@RequireRole(Roles.STUDENT)
public class VideoController {

    // 视频业务服务，构造器注入
    private final VideoService videoService;

    // 构造器注入 VideoService
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * 查询当前学生上传的全部视频。
     *
     * @return 视频列表
     */
    @GetMapping
    public ApiResponse<List<VideoDto>> list() {
        // 以当前登录学生为条件查询其视频
        return ApiResponse.ok(videoService.list(AuthContext.get()));
    }

    /**
     * 上传视频。
     *
     * @param title 视频标题（表单字段）
     * @param file  视频文件（multipart 部分，名为 file）
     * @return 上传成功后的视频信息
     */
    @PostMapping
    public ApiResponse<VideoDto> upload(@RequestParam String title,
                                        @RequestPart("file") MultipartFile file) {
        // 服务层完成文件存储与记录落库
        return ApiResponse.ok("视频已上传", videoService.upload(AuthContext.get(), title, file));
    }

    /**
     * 删除指定视频（仅视频归属学生）。
     *
     * @param id 视频 ID
     * @return 无数据的操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        // 服务层校验归属后删除记录并清理关联文件
        videoService.deleteOwned(id, AuthContext.get());
        return ApiResponse.ok("视频已删除", null);
    }

    /**
     * 查询指定视频详情。
     *
     * @param id 视频 ID
     * @return 视频详情
     */
    @GetMapping("/{id}")
    public ApiResponse<VideoDto> detail(@PathVariable Long id) {
        // 服务层校验归属后返回视频详情
        return ApiResponse.ok(videoService.detail(id, AuthContext.get()));
    }
}
