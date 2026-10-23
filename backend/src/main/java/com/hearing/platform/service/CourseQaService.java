package com.hearing.platform.service;

import com.hearing.platform.ai.AiResult;
import com.hearing.platform.ai.MockContent;
import com.hearing.platform.ai.PromptTemplates;
import com.hearing.platform.client.LlmClient;
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.config.AiProperties;
import com.hearing.platform.dto.CaptionDto;
import com.hearing.platform.dto.CourseQaRequest;
import com.hearing.platform.dto.CourseQaResponse;
import com.hearing.platform.dto.CourseQaSourceDto;
import com.hearing.platform.dto.MaterialDto;
import com.hearing.platform.dto.RoomDto;
import com.hearing.platform.entity.Lesson;
import com.hearing.platform.security.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CourseQaService {

    private static final Logger log = LoggerFactory.getLogger(CourseQaService.class);
    private static final int MAX_CHUNKS = 5;
    private static final int MAX_CONTEXT_CHARS = 6000;
    private static final int CHUNK_SIZE = 500;
    private static final DateTimeFormatter CAPTION_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Set<String> STOP_TERMS = Set.of(
            "什么", "怎么", "怎样", "为什么", "如何", "哪些", "请问", "一下", "可以", "是否",
            "这个", "那个", "我们", "你们", "老师", "课程", "内容", "问题", "需要", "根据"
    );
    private static final Pattern ENGLISH_TERM = Pattern.compile("[a-zA-Z0-9_]{2,}");
    private static final Pattern HAN_SEQUENCE = Pattern.compile("[\\p{IsHan}]{2,}");

    private final AiProperties properties;
    private final LlmClient llmClient;
    private final RoomService roomService;
    private final LessonService lessonService;
    private final MaterialService materialService;

    public CourseQaService(AiProperties properties, LlmClient llmClient, RoomService roomService,
                           LessonService lessonService, MaterialService materialService) {
        this.properties = properties;
        this.llmClient = llmClient;
        this.roomService = roomService;
        this.lessonService = lessonService;
        this.materialService = materialService;
    }

    public CourseQaResponse ask(CourseQaRequest request, AuthUser user) {
        if (!user.isStudent()) {
            throw BusinessException.forbidden("只有学生可以使用课程问答");
        }
        String question = request.question() == null ? "" : request.question().trim();
        if (question.isBlank()) {
            throw new BusinessException("请输入问题");
        }
        RoomDto room = roomService.getForUser(request.roomId(), user);
        Lesson lesson = lessonService.get(room.lessonId());
        List<CaptionDto> captions = roomService.captions(room.id(), user);
        MaterialDto classMaterial = materialService.findClassMaterialForQa(room.lessonId(), user);

        List<CourseChunk> chunks = new ArrayList<>();
        addTextChunks(chunks, "PPT", "PPT 内容", lesson.getPptText());
        addCaptionChunks(chunks, captions);
        if (classMaterial != null) {
            addTextChunks(chunks, "MATERIAL", "课堂学习资料", classMaterial.content());
        }
        if (chunks.isEmpty()) {
            throw new BusinessException("当前课程暂无可用于问答的资料");
        }

        List<String> terms = queryTerms(question);
        List<CourseChunk> selected = chunks.stream()
                .map(chunk -> chunk.withScore(score(chunk.content(), question, terms)))
                .filter(chunk -> chunk.score() > 0)
                .sorted(Comparator.comparingInt(CourseChunk::score).reversed())
                .limit(MAX_CHUNKS)
                .toList();
        if (selected.isEmpty()) {
            return new CourseQaResponse(
                    "当前课程资料中没有找到与该问题直接相关的内容，请尝试更具体的关键词。",
                    "LOCAL",
                    List.of());
        }

        List<CourseQaSourceDto> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            CourseChunk chunk = selected.get(i);
            String entry = "[S" + (i + 1) + "] " + chunk.label() + "\n" + chunk.content() + "\n\n";
            if (context.length() + entry.length() > MAX_CONTEXT_CHARS) break;
            context.append(entry);
            sources.add(new CourseQaSourceDto(chunk.type(), chunk.label(), abbreviate(chunk.content(), 220)));
        }

        String mode = mode();
        if ("mock".equals(mode)) {
            return mockResponse(question, sources);
        }
        try {
            String answer = llmClient.chat(PromptTemplates.system(),
                    PromptTemplates.courseQa(lesson.getTitle(), question, context.toString()));
            return new CourseQaResponse(answer, "AI", sources);
        } catch (RuntimeException ex) {
            if ("auto".equals(mode) && properties.isFallbackEnabled()) {
                log.warn("Course QA failed, using mock fallback: roomId={}, lessonId={}", room.id(), room.lessonId(), ex);
                return mockResponse(question, sources);
            }
            throw ex;
        }
    }

    private CourseQaResponse mockResponse(String question, List<CourseQaSourceDto> sources) {
        String excerpt = sources.isEmpty() ? "当前课程暂无足够资料。" : sources.get(0).excerpt();
        return new CourseQaResponse(MockContent.courseQa(question, excerpt), "MOCK", sources);
    }

    private void addTextChunks(List<CourseChunk> chunks, String type, String label, String text) {
        if (text == null || text.isBlank()) return;
        String normalized = text.replace("\r", " ").trim();
        for (int start = 0; start < normalized.length(); start += CHUNK_SIZE) {
            int end = Math.min(normalized.length(), start + CHUNK_SIZE);
            String content = normalized.substring(start, end).trim();
            if (!content.isBlank()) chunks.add(new CourseChunk(type, label, content, 0));
        }
    }

    private void addCaptionChunks(List<CourseChunk> chunks, List<CaptionDto> captions) {
        if (captions == null || captions.isEmpty()) return;
        StringBuilder group = new StringBuilder();
        String firstTime = "";
        int count = 0;
        for (CaptionDto caption : captions) {
            if (caption.content() == null || caption.content().isBlank()) continue;
            if (group.length() == 0 && caption.createdAt() != null) {
                firstTime = caption.createdAt().format(CAPTION_TIME);
            }
            if (group.length() + caption.content().length() > CHUNK_SIZE && group.length() > 0) {
                chunks.add(new CourseChunk("CAPTION", "课堂字幕 " + firstTime, group.toString(), 0));
                group.setLength(0);
                count = 0;
                firstTime = caption.createdAt() == null ? "" : caption.createdAt().format(CAPTION_TIME);
            }
            group.append(caption.content().trim()).append('\n');
            count += 1;
            if (count >= 4) {
                chunks.add(new CourseChunk("CAPTION", "课堂字幕 " + firstTime, group.toString().trim(), 0));
                group.setLength(0);
                count = 0;
            }
        }
        if (!group.isEmpty()) chunks.add(new CourseChunk("CAPTION", "课堂字幕 " + firstTime, group.toString().trim(), 0));
    }

    static List<String> queryTerms(String question) {
        String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
        Set<String> terms = new LinkedHashSet<>();
        Matcher english = ENGLISH_TERM.matcher(normalized);
        while (english.find()) addTerm(terms, english.group());
        Matcher han = HAN_SEQUENCE.matcher(normalized);
        while (han.find()) {
            String sequence = han.group();
            if (sequence.length() == 2) addTerm(terms, sequence);
            for (int i = 0; i + 2 <= sequence.length(); i++) addTerm(terms, sequence.substring(i, i + 2));
        }
        return List.copyOf(terms);
    }

    private static void addTerm(Set<String> terms, String term) {
        if (term != null && !term.isBlank() && !STOP_TERMS.contains(term)) terms.add(term);
    }

    static int score(String content, String question, List<String> terms) {
        String normalized = content == null ? "" : content.toLowerCase(Locale.ROOT);
        int score = normalized.contains(question.toLowerCase(Locale.ROOT).trim()) ? 8 : 0;
        for (String term : new HashSet<>(terms)) {
            score += countOccurrences(normalized, term) * Math.max(1, term.length() - 1);
        }
        return score;
    }

    private static int countOccurrences(String text, String term) {
        if (term.isBlank()) return 0;
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) >= 0) {
            count += 1;
            index += term.length();
        }
        return count;
    }

    private static String abbreviate(String value, int max) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max) + "…";
    }

    private String mode() {
        String value = properties.getMode() == null ? "auto" : properties.getMode().trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "real", "mock", "auto" -> value;
            default -> "auto";
        };
    }

    private record CourseChunk(String type, String label, String content, int score) {
        CourseChunk withScore(int value) { return new CourseChunk(type, label, content, value); }
    }
}
