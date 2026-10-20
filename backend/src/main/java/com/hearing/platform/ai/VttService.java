package com.hearing.platform.ai;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class VttService {

    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[。！？!?；;\\n])");
    private static final int MAX_CUE_CHARS = 80;

    public String createEstimatedVtt(String transcript, double durationSeconds) {
        String normalized = transcript == null ? "" : transcript.replace("\r", " ").replace("\n", "。").trim();
        List<String> cues = split(normalized);
        if (cues.isEmpty()) {
            cues.add("（未识别到有效字幕内容）");
        }
        double duration = Math.max(durationSeconds, 1.0);
        int totalChars = cues.stream().mapToInt(String::length).sum();
        int cumulative = 0;
        StringBuilder vtt = new StringBuilder("WEBVTT\n\n");
        for (int i = 0; i < cues.size(); i++) {
            String cue = cues.get(i);
            int startChars = cumulative;
            cumulative += cue.length();
            int endChars = cumulative;
            double start = duration * startChars / totalChars;
            double end = i == cues.size() - 1 ? duration : duration * endChars / totalChars;
            if (end <= start) {
                end = Math.min(duration, start + 0.5);
            }
            if (i == cues.size() - 1 && end < start + 0.5) {
                start = Math.max(0, duration - 0.5);
                end = duration;
            }
            vtt.append(formatTime(start)).append(" --> ").append(formatTime(end)).append('\n');
            vtt.append(cue).append("\n\n");
        }
        return vtt.toString();
    }

    private List<String> split(String text) {
        List<String> result = new ArrayList<>();
        for (String sentence : SENTENCE_BOUNDARY.split(text)) {
            String trimmed = sentence.trim();
            if (trimmed.isBlank() || !trimmed.matches(".*[\\p{L}\\p{N}].*")) {
                continue;
            }
            while (trimmed.length() > MAX_CUE_CHARS) {
                int splitAt = Math.min(MAX_CUE_CHARS, trimmed.length());
                result.add(trimmed.substring(0, splitAt).trim());
                trimmed = trimmed.substring(splitAt).trim();
            }
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private String formatTime(double seconds) {
        long millis = Math.round(Math.max(0, seconds) * 1000);
        long hours = millis / 3_600_000;
        millis %= 3_600_000;
        long minutes = millis / 60_000;
        millis %= 60_000;
        long secs = millis / 1000;
        long ms = millis % 1000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, secs, ms);
    }
}