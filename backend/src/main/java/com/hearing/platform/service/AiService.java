package com.hearing.platform.service;

import com.hearing.platform.ai.AiResult;          // AI 结果封装（内容 + 来源）
import com.hearing.platform.ai.MockContent;       // 降级用的模拟内容
import com.hearing.platform.ai.PromptTemplates;   // 提示词模板
import com.hearing.platform.client.AsrClient;     // 语音识别客户端
import com.hearing.platform.client.LlmClient;     // 大模型客户端
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.config.AiProperties;  // AI 配置（模式/回退/密钥等）
import com.hearing.platform.dto.MaterialDto;
import com.hearing.platform.dto.VideoDto;
import com.hearing.platform.entity.Caption;
import com.hearing.platform.entity.Lesson;
import com.hearing.platform.entity.Video;
import com.hearing.platform.security.AuthUser;    // 已认证用户

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * AI 业务服务层。
 * 封装备课资料生成、课堂学习资料生成、视频字幕转写及学习笔记生成等核心逻辑，
 * 并统一处理 LLM/ASR 的调用与降级（mock 回退）策略。
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    // AI 配置
    private final AiProperties properties;
    // 大模型客户端
    private final LlmClient llmClient;
    // 语音识别客户端
    private final AsrClient asrClient;
    // VTT 字幕生成服务
    private final VttService vttService;
    // 资料服务（落库）
    private final MaterialService materialService;
    // 课程服务（归属校验）
    private final LessonService lessonService;
    // 房间服务（字幕聚合/权限）
    private final RoomService roomService;
    // 视频服务（读写视频记录）
    private final VideoService videoService;
    // 媒体处理服务（抽音频/取时长）
    private final MediaProcessService mediaProcessService;
    // 文件存储服务
    private final FileStorageService storageService;

    // 构造器注入全部依赖
    public AiService(AiProperties properties, LlmClient llmClient, AsrClient asrClient,
                     VttService vttService,
                     MaterialService materialService, LessonService lessonService, RoomService roomService,
                     VideoService videoService, MediaProcessService mediaProcessService,
                     FileStorageService storageService) {
        this.properties = properties;
        this.llmClient = llmClient;
        this.asrClient = asrClient;
        this.vttService = vttService;
        this.materialService = materialService;
        this.lessonService = lessonService;
        this.roomService = roomService;
        this.videoService = videoService;
        this.mediaProcessService = mediaProcessService;
        this.storageService = storageService;
    }

    /**
     * 生成备课资料。
     * 校验课程 PPT 文本有效后，调用 LLM（依据 PPT 内容）生成资料，并写入素材库（类型：PREPARE）。
     *
     * @param lessonId 课程 ID
     * @param teacher  当前教师用户（需为该课程所有者）
     * @return 生成并持久化后的备课资料
     */
    @Transactional
    public MaterialDto prepare(Long lessonId, AuthUser teacher) {
        // 获取并校验课程归属于该教师
        Lesson lesson = lessonService.requireOwned(lessonId, teacher);
        // PPT 无可用文字则无法生成，直接报错
        if (lesson.getPptText() == null || lesson.getPptText().isBlank()) {
            throw new BusinessException("PPT 中没有可用于生成资料的文字");
        }
        // 调用 LLM 生成备课内容（失败时按策略回退 mock）
        AiResult result = runLlm(
                "PREPARE",
                lessonId,
                PromptTemplates.prepare(lesson.getTitle(), lesson.getPptText()),
                () -> MockContent.prepare(lesson.getTitle())
        );
        // 新增或更新对应类型的素材记录
        return materialService.upsert(lessonId, MaterialService.PREPARE, result.content(), result.source());
    }

    /**
     * 生成课堂学习资料。
     * 依据已结束课堂的字幕文本（结合 PPT）调用 LLM 生成资料，并写入素材库（类型：CLASS_MATERIAL）。
     * 教师可直接生成；学生必须存在已结束的课堂才能生成。
     *
     * @param lessonId 课程 ID
     * @param user     当前用户（教师或学生）
     * @return 生成并持久化后的课堂学习资料
     */
    @Transactional
    public MaterialDto classMaterial(Long lessonId, AuthUser user) {
        Lesson lesson;
        if (user.isTeacher()) {
            // 教师：校验课程归属后直接获取
            lesson = lessonService.requireOwned(lessonId, user);
        } else {
            // 学生：需存在已结束的课堂才允许生成
            if (!roomService.hasEndedRoomForLesson(lessonId, user.id())) {
                throw new BusinessException("课堂结束后才能生成学习资料");
            }
            lesson = lessonService.get(lessonId);
        }
        // 汇总该课程所有已结束课堂的字幕
        List<Caption> captions = roomService.allCaptionsForEndedRooms(lessonId);
        if (captions.isEmpty()) {
            throw new BusinessException("该课程还没有课堂字幕，无法生成学习资料");
        }
        // 将字幕内容拼接为单一文本
        String captionText = captions.stream().map(Caption::getContent).collect(Collectors.joining("\n"));
        // 基于 PPT 与字幕文本调用 LLM 生成学习资料
        AiResult result = runLlm(
                "CLASS_MATERIAL",
                lessonId,
                PromptTemplates.classMaterial(lesson.getTitle(), lesson.getPptText(), captionText),
                () -> MockContent.classMaterial(lesson.getTitle())
        );
        return materialService.upsert(lessonId, MaterialService.CLASS_MATERIAL, result.content(), result.source());
    }

    /**
     * 视频字幕转写。
     * 提取视频音频并经 ASR 转为文本，再根据视频时长生成（估算时间的）VTT 字幕文件，
     * 结果写回视频记录并将状态置为 TRANSCRIBED。若 ASR 失败且允许回退，则使用 mock 字幕。
     *
     * @param videoId 视频 ID
     * @param student 当前学生用户（需为该视频所有者）
     * @return 更新后的视频信息
     */
    @Transactional
    public VideoDto transcribe(Long videoId, AuthUser student) {
        Video video = videoService.requireOwned(videoId, student);
        // 标记为转写中状态
        video.setStatus("TRANSCRIBING");
        videoService.update(video);

        String audioRelative = null;
        double duration = 60.0;
        try {
            // 解析视频文件并获取总时长
            Path videoPath = storageService.resolve(video.getVideoPath());
            duration = mediaProcessService.durationSeconds(videoPath);
            Path audioPath;
            // 若尚未提取过音频（或音频文件丢失），则从视频抽取音频
            if (video.getAudioPath() == null || !Files.exists(storageService.resolve(video.getAudioPath()))) {
                String baseName = "video-" + video.getId() + "-" + UUID.randomUUID();
                audioPath = mediaProcessService.extractAudio(videoPath, storageService.root().resolve("audio"), baseName);
                // 将绝对路径转为相对存储根路径（统一使用 / 分隔）
                audioRelative = storageService.root().relativize(audioPath.toAbsolutePath().normalize())
                        .toString().replace('\\', '/');
            } else {
                // 已有音频则直接复用
                audioPath = storageService.resolve(video.getAudioPath());
                audioRelative = video.getAudioPath();
            }

            // ASR 转写音频为文本，并根据时长生成估算 VTT 字幕
            AiResult result = runAsr("VIDEO_TRANSCRIBE", videoId, audioPath, MockContent::transcript);
            String vtt = vttService.createEstimatedVtt(result.content(), duration);
            String vttRelative = storageService.writeText("vtt", vtt);
            // 回写音频、字幕、转写文本及相关字段
            video.setAudioPath(audioRelative);
            video.setVttPath(vttRelative);
            video.setTranscript(result.content());
            video.setTranscriptSource(result.source());
            video.setTimingMode("ESTIMATED");
            video.setStatus("TRANSCRIBED");
            videoService.update(video);
            return videoService.toDto(video);
        } catch (RuntimeException ex) {
            // 不允许回退时，置为失败并抛出异常
            if (!fallbackAllowed()) {
                video.setStatus("FAILED");
                videoService.update(video);
                throw ex;
            }
            // 允许回退：使用 mock 字幕保证流程可用
            log.warn("ASR failed, generating mock transcript: videoId={}", videoId, ex);
            AiResult fallback = new AiResult(MockContent.transcript(), "MOCK");
            String vtt = vttService.createEstimatedVtt(fallback.content(), duration);
            String vttRelative = storageService.writeText("vtt", vtt);
            video.setAudioPath(audioRelative);
            video.setVttPath(vttRelative);
            video.setTranscript(fallback.content());
            video.setTranscriptSource(fallback.source());
            video.setTimingMode("ESTIMATED");
            video.setStatus("TRANSCRIBED");
            videoService.update(video);
            return videoService.toDto(video);
        }
    }

    /**
     * 生成学习笔记。
     * 需视频已有字幕（转写文本），基于字幕调用 LLM 生成笔记，写回视频并将状态置为 NOTE_READY。
     *
     * @param videoId 视频 ID
     * @param student 当前学生用户（需为该视频所有者）
     * @return 更新后的视频信息（含学习笔记）
     */
    @Transactional
    public VideoDto note(Long videoId, AuthUser student) {
        Video video = videoService.requireOwned(videoId, student);
        // 未转写字幕则无法生成笔记
        if (video.getTranscript() == null || video.getTranscript().isBlank()) {
            throw new BusinessException("请先生成视频字幕");
        }
        // 基于视频字幕调用 LLM 生成学习笔记
        AiResult result = runLlm(
                "VIDEO_NOTE",
                videoId,
                PromptTemplates.videoNote(video.getTitle(), video.getTranscript()),
                () -> MockContent.videoNote(video.getTitle())
        );
        video.setNote(result.content());       // 写入笔记正文
        video.setNoteSource(result.source());  // 写入来源标记（AI/MOCK）
        video.setStatus("NOTE_READY");         // 置为笔记就绪
        video.setUpdatedAt(LocalDateTime.now());
        videoService.update(video);
        return videoService.toDto(video);
    }

    /**
     * 统一封装 LLM 调用与回退策略。
     * mode=mock 直接返回模拟内容；mode=real 严格调用 LLM（失败则抛出）；
     * mode=auto 优先调用 LLM，失败且允许回退时返回模拟内容。
     *
     * @param operation     业务操作类型，用于日志定位
     * @param resourceId    关联的课程或视频 ID
     * @param prompt        发送给 LLM 的提示词
     * @param mockSupplier  模拟内容的提供者（回退时使用）
     * @return 包含内容与来源标记（AI/MOCK）的结果
     */
    private AiResult runLlm(String operation, Long resourceId, String prompt, Supplier<String> mockSupplier) {
        String mode = mode();
        // mock 模式：不实际调用，直接返回模拟内容
        if ("mock".equals(mode)) {
            return new AiResult(mockSupplier.get(), "MOCK");
        }
        try {
            // 真实调用 LLM 对话接口
            return new AiResult(llmClient.chat(PromptTemplates.system(), prompt), "AI");
        } catch (RuntimeException ex) {
            // auto 模式且允许回退时，降级为模拟内容
            if ("auto".equals(mode) && properties.isFallbackEnabled()) {
                log.warn("LLM failed, using mock fallback: operation={}, resourceId={}", operation, resourceId, ex);
                return new AiResult(mockSupplier.get(), "MOCK");
            }
            throw ex;
        }
    }

    /**
     * 统一封装 ASR 调用与回退策略。
     * 回退逻辑与 {@link #runLlm} 一致：mock 直接返回，real 严格调用，auto 失败时降级。
     *
     * @param operation     业务操作类型，用于日志定位
     * @param resourceId    关联的视频 ID
     * @param audioFile     待转写的音频文件路径
     * @param mockSupplier  模拟转写文本的提供者（回退时使用）
     * @return 包含内容与来源标记（AI/MOCK）的结果
     */
    private AiResult runAsr(String operation, Long resourceId, Path audioFile, Supplier<String> mockSupplier) {
        String mode = mode();
        if ("mock".equals(mode)) {
            return new AiResult(mockSupplier.get(), "MOCK");
        }
        try {
            // 真实调用 ASR 语音识别接口
            return new AiResult(asrClient.transcribe(audioFile), "AI");
        } catch (RuntimeException ex) {
            if ("auto".equals(mode) && properties.isFallbackEnabled()) {
                log.warn("ASR failed, using mock fallback: operation={}, resourceId={}", operation, resourceId, ex);
                return new AiResult(mockSupplier.get(), "MOCK");
            }
            throw ex;
        }
    }

    /**
     * 判断当前是否允许回退到模拟内容（非 real 模式且配置开启回退）。
     */
    private boolean fallbackAllowed() {
        return !"real".equals(mode()) && properties.isFallbackEnabled();
    }

    /**
     * 获取并归一化 AI 运行模式。
     * 仅接受 real/mock/auto 三种取值，其他或未配置均归为 auto。
     */
    private String mode() {
        // 读取配置的模式，做空值兜底并统一为小写
        String mode = properties.getMode() == null ? "auto" : properties.getMode().trim().toLowerCase();
        // 仅接受三种合法取值，其余归一化为 auto
        return switch (mode) {
            case "real", "mock", "auto" -> mode;
            default -> "auto";
        };
    }
}
