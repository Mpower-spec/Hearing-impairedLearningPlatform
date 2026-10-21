package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;   // 统一响应封装体
import com.hearing.platform.dto.MaterialDto;       // 学习资料展示对象
import com.hearing.platform.security.AuthContext;  // 当前登录用户上下文
import com.hearing.platform.security.RequireRole;  // 角色访问控制注解
import com.hearing.platform.security.Roles;        // 角色常量定义
import com.hearing.platform.service.MaterialService; // 学习资料业务服务

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习资料接口控制器。
 * 提供备课资料与课堂学习资料的查询接口，统一前缀 /api/materials。
 * 注意：此处仅"读取"已生成的资料，生成动作在 AiController。
 */
@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    // 学习资料业务服务，构造器注入
    private final MaterialService materialService;

    // 构造器注入 MaterialService
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /**
     * 查询某课程的备课资料。
     * 权限：仅教师；且只能查看自己课程对应的资料。
     *
     * @param lessonId 课程 ID
     * @return 备课资料内容
     */
    @GetMapping("/prepare/{lessonId}")
    @RequireRole(Roles.TEACHER)
    public ApiResponse<MaterialDto> prepare(@PathVariable Long lessonId) {
        // 服务层校验课程归属后返回 PREPARE 类型资料
        return ApiResponse.ok(materialService.getPrepareMaterial(lessonId, AuthContext.get()));
    }

    /**
     * 查询某课程的课堂学习资料。
     * 权限：教师可看自己课程；学生需曾加入过该课程对应课堂。
     *
     * @param lessonId 课程 ID
     * @return 课堂学习资料内容
     */
    @GetMapping("/class/{lessonId}")
    public ApiResponse<MaterialDto> classMaterial(@PathVariable Long lessonId) {
        // 服务层根据角色做不同可见性校验后返回 CLASS_MATERIAL 类型资料
        return ApiResponse.ok(materialService.getClassMaterial(lessonId, AuthContext.get()));
    }
}
