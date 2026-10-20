package com.hearing.platform.material;

import com.hearing.platform.auth.AuthContext;
import com.hearing.platform.common.ApiResponse;
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
    @com.hearing.platform.auth.RequireRole(com.hearing.platform.auth.Roles.TEACHER)
    public ApiResponse<MaterialDto> prepare(@PathVariable Long lessonId) {
        return ApiResponse.ok(materialService.getPrepareMaterial(lessonId, AuthContext.get()));
    }

    @GetMapping("/class/{lessonId}")
    public ApiResponse<MaterialDto> classMaterial(@PathVariable Long lessonId) {
        return ApiResponse.ok(materialService.getClassMaterial(lessonId, AuthContext.get()));
    }
}