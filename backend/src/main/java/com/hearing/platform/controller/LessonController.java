package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;   // 统一响应封装体
import com.hearing.platform.dto.LessonDto;         // 课程信息展示对象
import com.hearing.platform.security.AuthContext;  // 当前登录用户上下文
import com.hearing.platform.security.RequireRole;  // 角色访问控制注解
import com.hearing.platform.security.Roles;        // 角色常量定义
import com.hearing.platform.service.LessonService; // 课程业务服务

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
 * 课程接口控制器。
 * 提供教师端的课程列表、上传（PPT）、删除、详情等接口，统一前缀 /api/lessons。
 * 类级 @RequireRole 限制：本控制器所有接口仅教师角色可访问。
 */
@RestController
@RequestMapping("/api/lessons")
@RequireRole(Roles.TEACHER)
public class LessonController {

    // 课程业务服务，构造器注入
    private final LessonService lessonService;

    // 构造器注入 LessonService
    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    /**
     * 查询当前教师的全部课程列表。
     *
     * @return 课程列表
     */
    @GetMapping
    public ApiResponse<List<LessonDto>> list() {
        // 以当前登录教师为条件查询其名下课程
        return ApiResponse.ok(lessonService.listByTeacher(AuthContext.get()));
    }

    /**
     * 上传 PPT 创建课程。
     *
     * @param title 课程名称（表单字段）
     * @param file  上传的 PPTX 文件（multipart 部分，名为 file）
     * @return 创建成功的课程信息
     */
    @PostMapping
    public ApiResponse<LessonDto> upload(@RequestParam String title,
                                         @RequestPart("file") MultipartFile file) {
        // 服务层完成存储、转 PDF、提取文字并落库
        return ApiResponse.ok("课程已创建", lessonService.upload(AuthContext.get(), title, file));
    }

    /**
     * 删除指定课程（仅课程归属教师）。
     *
     * @param id 课程 ID
     * @return 无数据的操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        // 服务层校验归属并级联删除关联数据
        lessonService.deleteOwned(id, AuthContext.get());
        return ApiResponse.ok("课程已删除", null);
    }

    /**
     * 查询指定课程详情。
     *
     * @param id 课程 ID
     * @return 课程详情
     */
    @GetMapping("/{id}")
    public ApiResponse<LessonDto> detail(@PathVariable Long id) {
        // 服务层校验归属后返回课程详情
        return ApiResponse.ok(lessonService.detail(id, AuthContext.get()));
    }
}
