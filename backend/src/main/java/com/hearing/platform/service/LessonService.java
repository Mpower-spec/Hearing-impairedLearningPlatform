package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.dao.CaptionMapper;        // 字幕数据访问
import com.hearing.platform.dao.DanmakuMapper;        // 弹幕数据访问
import com.hearing.platform.dao.LessonMapper;         // 课程数据访问
import com.hearing.platform.dao.MaterialMapper;       // 资料数据访问
import com.hearing.platform.dao.RoomMapper;           // 房间数据访问
import com.hearing.platform.dao.RoomMemberMapper;     // 房间成员数据访问
import com.hearing.platform.dto.LessonDto;            // 课程展示对象
import com.hearing.platform.entity.Caption;
import com.hearing.platform.entity.Danmaku;
import com.hearing.platform.entity.Lesson;
import com.hearing.platform.entity.Material;
import com.hearing.platform.entity.Room;
import com.hearing.platform.entity.RoomMember;
import com.hearing.platform.security.AuthUser;        // 已认证用户

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 课程业务服务。
 * 负责 PPT 上传后的转换流水线（转 PDF、提取文字、统计页数）与课程增删查，
 * 并在删除课程时级联清理关联的课堂、成员、字幕、弹幕、资料及磁盘文件。
 */
@Service
public class LessonService {

    // PPTX 大小上限：50MB
    private static final long MAX_PPTX_BYTES = 50L * 1024 * 1024;

    // 课程数据访问
    private final LessonMapper lessonMapper;
    // PPT 转换服务
    private final PptConversionService conversionService;
    // 文件存储服务
    private final FileStorageService storageService;
    // 以下 Mapper 用于删除课程时的级联清理
    private final RoomMapper roomMapper;
    private final RoomMemberMapper roomMemberMapper;
    private final CaptionMapper captionMapper;
    private final DanmakuMapper danmakuMapper;
    private final MaterialMapper materialMapper;

    // 构造器注入依赖
    public LessonService(LessonMapper lessonMapper, PptConversionService conversionService,
                         FileStorageService storageService, RoomMapper roomMapper,
                         RoomMemberMapper roomMemberMapper, CaptionMapper captionMapper,
                         DanmakuMapper danmakuMapper, MaterialMapper materialMapper) {
        this.lessonMapper = lessonMapper;
        this.conversionService = conversionService;
        this.storageService = storageService;
        this.roomMapper = roomMapper;
        this.roomMemberMapper = roomMemberMapper;
        this.captionMapper = captionMapper;
        this.danmakuMapper = danmakuMapper;
        this.materialMapper = materialMapper;
    }

    /**
     * 上传 PPT 创建课程。
     * 流程：存储 PPTX → 转 PDF → 提取文字 → 统计页数 → 落库；任一步失败则清理已存文件。
     *
     * @param teacher 当前教师
     * @param title   课程名称
     * @param file    PPTX 文件
     * @return 课程信息
     */
    @Transactional
    public LessonDto upload(AuthUser teacher, String title, MultipartFile file) {
        // 标题为空则拒绝
        if (title == null || title.isBlank()) {
            throw new BusinessException("请输入课程名称");
        }
        // 先存储原始 PPTX（仅允许 pptx）
        String pptxRelative = storageService.store(file, "pptx", Set.of("pptx"), MAX_PPTX_BYTES);
        String pdfRelative = null;
        try {
            // 定位 PPTX 并转换为 PDF
            Path pptxPath = storageService.resolve(pptxRelative);
            Path pdfPath = conversionService.convertToPdf(pptxPath, storageService.root().resolve("pdf"));
            // 将 PDF 绝对路径转为相对存储根路径（统一使用 / 分隔）
            pdfRelative = storageService.root().relativize(pdfPath.toAbsolutePath().normalize()).toString().replace('\\', '/');
            // 提取 PPT 文本、统计 PDF 页数
            String text = conversionService.extractText(pptxPath);
            int pageCount = conversionService.countPdfPages(pdfPath);

            // 组装课程记录
            Lesson lesson = new Lesson();
            lesson.setTeacherId(teacher.id());
            lesson.setTitle(title.trim());
            lesson.setPptxPath(pptxRelative);
            lesson.setPdfPath(pdfRelative);
            lesson.setPptText(text);
            lesson.setPageCount(pageCount);
            lesson.setStatus("READY");        // 转换完成、可用于上课
            lesson.setCreatedAt(LocalDateTime.now());
            lessonMapper.insert(lesson);
            return LessonDto.from(lesson, storageService.url(pdfRelative));
        } catch (RuntimeException ex) {
            // 失败回滚：清理可能已写入的 PPTX 与 PDF，再抛出
            storageService.deleteQuietly(pptxRelative);
            storageService.deleteQuietly(pdfRelative);
            throw ex;
        }
    }

    /**
     * 查询当前教师的全部课程（按创建时间倒序）。
     */
    public List<LessonDto> listByTeacher(AuthUser teacher) {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getTeacherId, teacher.id())
                        .orderByDesc(Lesson::getCreatedAt))
                .stream()
                .map(lesson -> LessonDto.from(lesson, storageService.url(lesson.getPdfPath())))
                .toList();
    }

    /**
     * 课程详情（含归属校验）。
     */
    public LessonDto detail(Long lessonId, AuthUser teacher) {
        Lesson lesson = requireOwned(lessonId, teacher);
        return LessonDto.from(lesson, storageService.url(lesson.getPdfPath()));
    }

    /**
     * 获取课程并校验归属；不存在 404，非本人 403。
     */
    public Lesson requireOwned(Long lessonId, AuthUser teacher) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (!lesson.getTeacherId().equals(teacher.id())) {
            throw BusinessException.forbidden("无权访问该课程");
        }
        return lesson;
    }

    /**
     * 删除课程：校验归属后级联删除弹幕、字幕、成员、房间、资料，
     * 最后删除课程记录与磁盘上的 PPTX/PDF 文件。
     */
    @Transactional
    public void deleteOwned(Long lessonId, AuthUser teacher) {
        Lesson lesson = requireOwned(lessonId, teacher);
        // 找出该课程下的所有课堂
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getLessonId, lessonId));
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        if (!roomIds.isEmpty()) {
            // 按依赖顺序删除：弹幕 → 字幕 → 成员 → 房间
            danmakuMapper.delete(new LambdaQueryWrapper<Danmaku>().in(Danmaku::getRoomId, roomIds));
            captionMapper.delete(new LambdaQueryWrapper<Caption>().in(Caption::getRoomId, roomIds));
            roomMemberMapper.delete(new LambdaQueryWrapper<RoomMember>().in(RoomMember::getRoomId, roomIds));
            roomMapper.delete(new LambdaQueryWrapper<Room>().eq(Room::getLessonId, lessonId));
        }
        // 删除该课程的资料与课程本身
        materialMapper.delete(new LambdaQueryWrapper<Material>().eq(Material::getLessonId, lessonId));
        lessonMapper.deleteById(lessonId);

        // 清理磁盘文件（静默失败）
        storageService.deleteQuietly(lesson.getPptxPath());
        storageService.deleteQuietly(lesson.getPdfPath());
    }

    /**
     * 按 ID 获取课程（不做归属校验），不存在则 404。
     */
    public Lesson get(Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        return lesson;
    }
}
