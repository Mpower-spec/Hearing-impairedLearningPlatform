package com.hearing.platform.lesson;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hearing.platform.auth.AuthUser;
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.storage.FileStorageService;
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

    public LessonService(LessonMapper lessonMapper, PptConversionService conversionService,
                         FileStorageService storageService) {
        this.lessonMapper = lessonMapper;
        this.conversionService = conversionService;
        this.storageService = storageService;
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

    public Lesson get(Long lessonId) {
        Lesson lesson = lessonMapper.selectById(lessonId);
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        return lesson;
    }
}