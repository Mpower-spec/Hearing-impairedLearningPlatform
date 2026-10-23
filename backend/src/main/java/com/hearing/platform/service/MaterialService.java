package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.dao.MaterialMapper;       // 学习资料数据访问
import com.hearing.platform.dto.MaterialDto;          // 学习资料展示对象
import com.hearing.platform.entity.Material;          // 学习资料实体
import com.hearing.platform.security.AuthUser;        // 已认证用户

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 学习资料业务服务。
 * 管理两类资料：备课资料（PREPARE）与课堂学习资料（CLASS_MATERIAL），
 * 以 (lessonId, type) 为唯一维度做新增或覆盖，并提供带权限校验的查询。
 */
@Service
public class MaterialService {

    // 资料类型：备课
    public static final String PREPARE = "PREPARE";
    // 资料类型：课堂学习
    public static final String CLASS_MATERIAL = "CLASS_MATERIAL";

    // 资料数据访问
    private final MaterialMapper materialMapper;
    // 课程服务（用于归属校验）
    private final LessonService lessonService;
    // 房间服务（用于学生可见性校验）
    private final RoomService roomService;

    // 构造器注入依赖
    public MaterialService(MaterialMapper materialMapper, LessonService lessonService, RoomService roomService) {
        this.materialMapper = materialMapper;
        this.lessonService = lessonService;
        this.roomService = roomService;
    }

    /**
     * 新增或覆盖某课程某类型的资料（存在则更新，否则插入）。
     *
     * @param lessonId 课程 ID
     * @param type     资料类型（PREPARE / CLASS_MATERIAL）
     * @param content  资料正文
     * @param source   来源标记（AI / MOCK）
     * @return 资料信息
     */
    @Transactional
    public MaterialDto upsert(Long lessonId, String type, String content, String source) {
        // 先按 (lessonId, type) 查询是否已存在
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getLessonId, lessonId).eq(Material::getType, type));
        LocalDateTime now = LocalDateTime.now();
        if (material == null) {
            // 不存在：新建记录
            material = new Material();
            material.setLessonId(lessonId);
            material.setType(type);
            material.setContent(content);
            material.setSource(source);
            material.setCreatedAt(now);
            material.setUpdatedAt(now);
            materialMapper.insert(material);
        } else {
            // 已存在：覆盖内容与来源并刷新更新时间
            material.setContent(content);
            material.setSource(source);
            material.setUpdatedAt(now);
            materialMapper.updateById(material);
        }
        return MaterialDto.from(material);
    }

    /**
     * 查询备课资料：仅课程归属教师可见。
     *
     * @param lessonId 课程 ID
     * @param teacher  当前教师
     * @return 备课资料
     */
    public MaterialDto getPrepareMaterial(Long lessonId, AuthUser teacher) {
        // 校验课程归属该教师
        lessonService.requireOwned(lessonId, teacher);
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getLessonId, lessonId).eq(Material::getType, PREPARE));
        // 尚未生成则返回 404
        if (material == null) {
            throw BusinessException.notFound("备课资料尚未生成");
        }
        return MaterialDto.from(material);
    }

    /**
     * 查询课堂学习资料。
     * 教师需为课程归属者；学生需曾加入过该课程的任一课堂。
     *
     * @param lessonId 课程 ID
     * @param user     当前用户
     * @return 课堂学习资料
     */
    public MaterialDto findClassMaterialForQa(Long lessonId, AuthUser user) {
        if (user.isTeacher()) {
            lessonService.requireOwned(lessonId, user);
        } else if (!roomService.isMemberOfLesson(lessonId, user.id())) {
            throw BusinessException.forbidden("加入过该课堂的学生才能使用课程问答");
        }
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getLessonId, lessonId).eq(Material::getType, CLASS_MATERIAL));
        return material == null ? null : MaterialDto.from(material);
    }

    public MaterialDto getClassMaterial(Long lessonId, AuthUser user) {
        if (user.isTeacher()) {
            // 教师：校验课程归属
            lessonService.requireOwned(lessonId, user);
        } else if (!roomService.isMemberOfLesson(lessonId, user.id())) {
            // 学生：必须加入过该课程对应课堂
            throw BusinessException.forbidden("加入过该课堂的学生才能查看资料");
        }
        Material material = materialMapper.selectOne(new LambdaQueryWrapper<Material>()
                .eq(Material::getLessonId, lessonId).eq(Material::getType, CLASS_MATERIAL));
        // 尚未生成则返回 404
        if (material == null) {
            throw BusinessException.notFound("课堂学习资料尚未生成");
        }
        return MaterialDto.from(material);
    }
}
