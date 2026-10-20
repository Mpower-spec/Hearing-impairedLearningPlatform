package com.hearing.platform.material;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hearing.platform.auth.AuthUser;
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.lesson.LessonService;
import com.hearing.platform.room.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MaterialService {

    public static final String PREPARE = "PREPARE";
    public static final String CLASS_MATERIAL = "CLASS_MATERIAL";

    private final MaterialMapper materialMapper;
    private final LessonService lessonService;
    private final RoomService roomService;

    public MaterialService(MaterialMapper materialMapper, LessonService lessonService, RoomService roomService) {
        this.materialMapper = materialMapper;
        this.lessonService = lessonService;
        this.roomService = roomService;
    }

    @Transactional
    public MaterialDto upsert(Long lessonId, String type, String content, String source) {
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getLessonId, lessonId).eq(Material::getType, type));
        LocalDateTime now = LocalDateTime.now();
        if (material == null) {
            material = new Material();
            material.setLessonId(lessonId);
            material.setType(type);
            material.setContent(content);
            material.setSource(source);
            material.setCreatedAt(now);
            material.setUpdatedAt(now);
            materialMapper.insert(material);
        } else {
            material.setContent(content);
            material.setSource(source);
            material.setUpdatedAt(now);
            materialMapper.updateById(material);
        }
        return MaterialDto.from(material);
    }

    public MaterialDto getPrepareMaterial(Long lessonId, AuthUser teacher) {
        lessonService.requireOwned(lessonId, teacher);
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getLessonId, lessonId).eq(Material::getType, PREPARE));
        if (material == null) {
            throw BusinessException.notFound("备课资料尚未生成");
        }
        return MaterialDto.from(material);
    }

    public MaterialDto getClassMaterial(Long lessonId, AuthUser user) {
        if (user.isTeacher()) {
            lessonService.requireOwned(lessonId, user);
        } else if (!roomService.isMemberOfLesson(lessonId, user.id())) {
            throw BusinessException.forbidden("加入过该课堂的学生才能查看资料");
        }
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getLessonId, lessonId).eq(Material::getType, CLASS_MATERIAL));
        if (material == null) {
            throw BusinessException.notFound("课堂学习资料尚未生成");
        }
        return MaterialDto.from(material);
    }
}