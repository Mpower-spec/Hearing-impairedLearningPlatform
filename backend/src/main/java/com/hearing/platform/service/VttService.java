package com.hearing.platform.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * VTT 字幕服务。
 * 由于 ASR 仅返回纯文本、不带时间戳，这里依据视频总时长与每条字幕的字符数
 * 按比例"估算"出时间轴，生成标准 WEBVTT 文本（timing 为 ESTIMATED）。
 */
@Service
public class VttService {

    // 句子切分点：在中文/英文句末标点或换行之后断开（零宽断言，保留标点）
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[。！？!?；;\\n])");
    // 单条字幕最大字符数，超出会继续强制切分，避免一条过长
    private static final int MAX_CUE_CHARS = 80;

    /**
     * 生成估算时间轴的 VTT 文本。
     *
     * @param transcript     ASR 转写的纯文本
     * @param durationSeconds 视频总时长（秒）
     * @return WEBVTT 文本
     */
    public String createEstimatedVtt(String transcript, double durationSeconds) {
        // 规范化：去除回车，将换行统一替换为句号，便于按句切分
        String normalized = transcript == null ? "" : transcript.replace("\r", " ").replace("\n", "。").trim();
        // 切分为若干字幕条目
        List<String> cues = split(normalized);
        // 无任何有效条目时给一条占位字幕，保证 VTT 合法
        if (cues.isEmpty()) {
            cues.add("（未识别到有效字幕内容）");
        }
        // 时长兜底至少 1 秒，避免除零或负值
        double duration = Math.max(durationSeconds, 1.0);
        // 所有字幕的总字符数，用于按比例分配时间
        int totalChars = cues.stream().mapToInt(String::length).sum();
        int cumulative = 0; // 已累计字符数（用于计算每条的起始位置）
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        for (int i = 0; i < cues.size(); i++) {
            String cue = cues.get(i);
            int startChars = cumulative;            // 本条起始字符累计
            cumulative += cue.length();             // 累加本条长度
            int endChars = cumulative;              // 本条结束字符累计
            // 按"字符占比"换算为时间（秒）
            double start = duration * startChars / totalChars;
            // 最后一条的结束时间对齐到视频末尾，其余按比例
            double end = i == cues.size() - 1 ? duration : duration * endChars / totalChars;
            // 防止零时长：结束不晚于开始时，至少给 0.5 秒
            if (end <= start) {
                end = Math.min(duration, start + 0.5);
            }
            // 最后一条过短时，回溯起始时间确保其时长不少于 0.5 秒
            if (i == cues.size() - 1 && end < start + 0.5) {
                start = Math.max(0, duration - 0.5);
                end = duration;
            }
            // 写入时间轴行与字幕文本行
            vtt.append(formatTime(start)).append(" --> ").append(formatTime(end)).append('\n');
            vtt.append(cue).append("\n\n");
        }
        return vtt.toString();
    }

    /**
     * 将文本切分为字幕条目列表。
     * 先按句子边界拆分，过滤无字/无数的空白条目，再对过长句子按上限二次切分。
     */
    private List<String> split(String text) {
        List<String> result = new ArrayList<>();
        for (String sentence : SENTENCE_BOUNDARY.split(text)) {
            String trimmed = sentence.trim();
            // 过滤空串及不含任何字母/数字的句子（纯标点等）
            if (trimmed.isBlank() || !trimmed.matches(".*[\\p{L}\\p{N}].*")) {
                continue;
            }
            // 超过最大长度的句子按固定步长继续切分
            while (trimmed.length() > MAX_CUE_CHARS) {
                int splitAt = Math.min(MAX_CUE_CHARS, trimmed.length());
                result.add(trimmed.substring(0, splitAt).trim());
                trimmed = trimmed.substring(splitAt).trim();
            }
            // 追加剩余片段
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * 将秒数格式化为 VTT 时间戳：HH:mm:ss.SSS。
     */
    private String formatTime(double seconds) {
        // 转为毫秒并做非负兜底
        long millis = Math.round(Math.max(0, seconds) * 1000);
        long hours = millis / 3_600_000;   // 小时
        millis %= 3_600_000;
        long minutes = millis / 60_000;    // 分钟
        millis %= 60_000;
        long secs = millis / 1000;         // 秒
        long ms = millis % 1000;           // 剩余毫秒
        // 使用 Locale.ROOT 保证分隔符稳定为 : 和 .
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, secs, ms);
    }
}
