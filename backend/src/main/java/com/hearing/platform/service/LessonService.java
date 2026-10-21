package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException;
import com.hearing.platform.dao.CaptionMapper;
import com.hearing.platform.dao.DanmakuMapper;
import com.hearing.platform.dao.LessonMapper;
import com.hearing.platform.dao.MaterialMapper;
import com.hearing.platform.dao.RoomMapper;
import com.hearing.platform.dao.RoomMemberMapper;
import com.hearing.platform.dto.LessonDto;
import com.hearing.platform.entity.Caption;
import com.hearing.platform.entity.Danmaku;
import com.hearing.platform.entity.Lesson;
import com.hearing.platform.entity.Material;
import com.hearing.platform.entity.Room;
import com.hearing.platform.entity.RoomMember;
import com.hearing.platform.security.AuthUser;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class LessonService {

    private static final long MAX_PPTX_BYTES = 50L * 1024 * 1024;

    private final LessonMapper lessonMapper;
    private final PptConversionService conversionService;
    private final FileStorageService storageService;
    private final RoomMapper roomMapper;
    private final RoomMemberMapper roomMemberMapper;
    private final CaptionMapper captionMapper;
    private final DanmakuMapper danmakuMapper;
    private final MaterialMapper materialMapper;

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

    @Transactional
    public LessonDto upload(AuthUser teacher, String title, MultipartFile file) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("请输入课程名称");
        }
        String pptxRelative = storageService.store(file, "pptx", Set.of("pptx"), MAX_PPTX_BYTES);
        String pdfRelative = null;
        try {
            Path pptxPath = storageService.resolve(pptxRelative);
            Path pdfPath = conversionService.convertToPdf(pptxPath, storageService.root().resolve("pdf"));
            pdfRelative = storageService.root().relativize(pdfPath.toAbsolutePath().normalize()).toString().replace('\\', '/');
            String text = conversionService.extractText(pptxPath);
            int pageCount = conversionService.countPdfPages(pdfPath);

            Lesson lesson = new Lesson();
            lesson.setTeacherId(teacher.id());
            lesson.setTitle(title.trim());
            lesson.setPptxPath(pptxRelative);
            lesson.setPdfPath(pdfRelative);
            lesson.setPptText(text);
            lesson.setPageCount(pageCount);
            lesson.setStatus("READY");
            lesson.setCreatedAt(LocalDateTime.now());
            lessonMapper.insert(lesson);
            return LessonDto.from(lesson, storageService.url(pdfRelative));
        } catch (RuntimeException ex) {
            storageService.deleteQuietly(pptxRelative);
            storageService.deleteQuietly(pdfRelative);
            throw ex;
        }
    }

    public List<LessonDto> listByTeacher(AuthUser teacher) {
        return lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                        .eq(Lesson::getTeacherId, teacher.id())
                        .orderByDesc(Lesson::getCreatedAt))
                .stream()
                .map(lesson -> LessonDto.from(lesson, storageService.url(lesson.getPdfPath())))
                .toList();
    }

    public LessonDto detail(Long lessonId, AuthUser teacher) {
        Lesson lesson = requireOwned(lessonId, teacher);
        return LessonDto.from(lesson, storageService.url(lesson.getPdfPath()));
    }

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

    @Transactional
    public void deleteOwned(Long lessonId, AuthUser teacher) {
        Lesson lesson = requireOwned(lessonId, teacher);
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getLessonId, lessonId));
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        if (!roomIds.isEmpty()) {
            danmakuMapper.delete(new LambdaQueryWrapper<Danmaku>().in(Danmaku::getRoomId, roomIds));
            captionMapper.delete(new LambdaQueryWrapper<Caption>().in(Caption::getRoomId, roomIds));
            roomMemberMapper.delete(new LambdaQueryWrapper<RoomMember>().in(RoomMember::getRoomId, roomIds));
            roomMapper.delete(new LambdaQueryWrapper<Room>().eq(Room::getLessonId, lessonId));
        }
        materialMapper.delete(new LambdaQueryWrapper<Material>().eq(Material::getLessonId, lessonId));
        lessonMapper.deleteById(lessonId);

        storageService.deleteQuietly(lesson.getPptxPath());
        storageService.deleteQuietly(lesson.getPdfPath());
    }

    public Lesson get(Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        return lesson;
    }
}