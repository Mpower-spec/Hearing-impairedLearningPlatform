package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;
import com.hearing.platform.dto.MaterialDto;
import com.hearing.platform.security.AuthContext;
import com.hearing.platform.security.RequireRole;
import com.hearing.platform.security.Roles;
import com.hearing.platform.service.MaterialService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/materials")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/prepare/{lessonId}")
    @com.hearing.platform.security.RequireRole(com.hearing.platform.security.Roles.TEACHER)
    public ApiResponse<MaterialDto> prepare(@PathVariable Long lessonId) {
        return ApiResponse.ok(materialService.getPrepareMaterial(lessonId, AuthContext.get()));
    }

    @GetMapping("/class/{lessonId}")
    public ApiResponse<MaterialDto> classMaterial(@PathVariable Long lessonId) {
        return ApiResponse.ok(materialService.getClassMaterial(lessonId, AuthContext.get()));
    }
}