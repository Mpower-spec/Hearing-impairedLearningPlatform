package com.hearing.platform.ai;

import com.hearing.platform.auth.AuthUser;
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.config.AiProperties;
import com.hearing.platform.lesson.Lesson;
import com.hearing.platform.lesson.LessonService;
import com.hearing.platform.material.MaterialDto;
import com.hearing.platform.material.MaterialService;
import com.hearing.platform.room.Caption;
import com.hearing.platform.room.RoomService;
import com.hearing.platform.storage.FileStorageService;
import com.hearing.platform.video.MediaProcessService;
import com.hearing.platform.video.Video;
import com.hearing.platform.video.VideoDto;
import com.hearing.platform.video.VideoService;
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

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiProperties properties;
    private final LlmClient llmClient;
    private final AsrClient asrClient;
    private final VttService vttService;
    private final MaterialService materialService;
    private final LessonService lessonService;
    private final RoomService roomService;
    private final VideoService videoService;
    private final MediaProcessService mediaProcessService;
    private final FileStorageService storageService;

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

    @Transactional
    public MaterialDto prepare(Long lessonId, AuthUser teacher) {
        Lesson lesson = lessonService.requireOwned(lessonId, teacher);
        if (lesson.getPptText() == null || lesson.getPptText().isBlank()) {
            throw new BusinessException("PPT 中没有可用于生成资料的文字");
        }
        AiResult result = runLlm(
                PromptTemplates.prepare(lesson.getTitle(), lesson.getPptText()),
                () -> MockContent.prepare(lesson.getTitle())
        );
        return materialService.upsert(lessonId, MaterialService.PREPARE, result.content(), result.source());
    }

    @Transactional
    public MaterialDto classMaterial(Long lessonId, AuthUser user) {
        Lesson lesson;
        if (user.isTeacher()) {
            lesson = lessonService.requireOwned(lessonId, user);
        } else {
            if (!roomService.hasEndedRoomForLesson(lessonId, user.id())) {
                throw new BusinessException("课堂结束后才能生成学习资料");
            }
            lesson = lessonService.get(lessonId);
        }
        List<Caption> captions = roomService.allCaptionsForEndedRooms(lessonId);
        if (captions.isEmpty()) {
            throw new BusinessException("该课程还没有课堂字幕，无法生成学习资料");
        }
        String captionText = captions.stream().map(Caption::getContent).collect(Collectors.joining("\n"));
        AiResult result = runLlm(
                PromptTemplates.classMaterial(lesson.getTitle(), lesson.getPptText(), captionText),
                () -> MockContent.classMaterial(lesson.getTitle())
        );
        return materialService.upsert(lessonId, MaterialService.CLASS_MATERIAL, result.content(), result.source());
    }

    @Transactional
    public VideoDto transcribe(Long videoId, AuthUser student) {
        Video video = videoService.requireOwned(videoId, student);
        video.setStatus("TRANSCRIBING");
        videoService.update(video);

        String audioRelative = null;
        double duration = 60.0;
        try {
            Path videoPath = storageService.resolve(video.getVideoPath());
            duration = mediaProcessService.durationSeconds(videoPath);
            Path audioPath;
            if (video.getAudioPath() == null || !Files.exists(storageService.resolve(video.getAudioPath()))) {
                String baseName = "video-" + video.getId() + "-" + UUID.randomUUID();
                audioPath = mediaProcessService.extractAudio(videoPath, storageService.root().resolve("audio"), baseName);
                audioRelative = storageService.root().relativize(audioPath.toAbsolutePath().normalize())
                        .toString().replace('\\', '/');
            } else {
                audioPath = storageService.resolve(video.getAudioPath());
                audioRelative = video.getAudioPath();
            }

            AiResult result = runAsr(audioPath, MockContent::transcript);
            String vtt = vttService.createEstimatedVtt(result.content(), duration);
            String vttRelative = storageService.writeText("vtt", vtt);
            video.setAudioPath(audioRelative);
            video.setVttPath(vttRelative);
            video.setTranscript(result.content());
            video.setTranscriptSource(result.source());
            video.setTimingMode("ESTIMATED");
            video.setStatus("TRANSCRIBED");
            videoService.update(video);
            return videoService.toDto(video);
        } catch (RuntimeException ex) {
            if (!fallbackAllowed()) {
                video.setStatus("FAILED");
                videoService.update(video);
                throw ex;
            }
            log.warn("ASR failed, generating mock transcript", ex);
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

    @Transactional
    public VideoDto note(Long videoId, AuthUser student) {
        Video video = videoService.requireOwned(videoId, student);
        if (video.getTranscript() == null || video.getTranscript().isBlank()) {
            throw new BusinessException("请先生成视频字幕");
        }
        AiResult result = runLlm(
                PromptTemplates.videoNote(video.getTitle(), video.getTranscript()),
                () -> MockContent.videoNote(video.getTitle())
        );
        video.setNote(result.content());
        video.setNoteSource(result.source());
        video.setStatus("NOTE_READY");
        video.setUpdatedAt(LocalDateTime.now());
        videoService.update(video);
        return videoService.toDto(video);
    }

    private AiResult runLlm(String prompt, Supplier<String> mockSupplier) {
        String mode = mode();
        if ("mock".equals(mode)) {
            return new AiResult(mockSupplier.get(), "MOCK");
        }
        try {
            return new AiResult(llmClient.chat(PromptTemplates.system(), prompt), "AI");
        } catch (RuntimeException ex) {
            if ("auto".equals(mode) && properties.isFallbackEnabled()) {
                log.warn("LLM failed, using mock fallback", ex);
                return new AiResult(mockSupplier.get(), "MOCK");
            }
            throw ex;
        }
    }

    private AiResult runAsr(Path audioFile, Supplier<String> mockSupplier) {
        String mode = mode();
        if ("mock".equals(mode)) {
            return new AiResult(mockSupplier.get(), "MOCK");
        }
        try {
            return new AiResult(asrClient.transcribe(audioFile), "AI");
        } catch (RuntimeException ex) {
            if ("auto".equals(mode) && properties.isFallbackEnabled()) {
                log.warn("ASR failed, using mock fallback", ex);
                return new AiResult(mockSupplier.get(), "MOCK");
            }
            throw ex;
        }
    }

    private boolean fallbackAllowed() {
        return !"real".equals(mode()) && properties.isFallbackEnabled();
    }

    private String mode() {
        String mode = properties.getMode() == null ? "auto" : properties.getMode().trim().toLowerCase();
        return switch (mode) {
            case "real", "mock", "auto" -> mode;
            default -> "auto";
        };
    }
}