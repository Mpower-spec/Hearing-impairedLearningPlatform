package com.hearing.platform.video;

import com.hearing.platform.common.BusinessException;
import com.hearing.platform.config.AppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MediaProcessService {

    private final AppProperties properties;

    public MediaProcessService(AppProperties properties) {
        this.properties = properties;
    }

    public Path extractAudio(Path video, Path outputDirectory, String baseName) {
        try {
            Files.createDirectories(outputDirectory);
            Path output = outputDirectory.resolve(baseName + ".mp3");
            run(List.of(properties.getFfmpegPath(), "-y", "-i", video.toAbsolutePath().toString(),
                    "-vn", "-ac", "1", "-ar", "16000", "-c:a", "libmp3lame", "-b:a", "64k", output.toAbsolutePath().toString()), 240);
            if (!Files.exists(output) || Files.size(output) == 0) {
                throw new BusinessException("FFmpeg 未生成音频文件");
            }
            return output;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "视频抽取音频失败：" + ex.getMessage());
        }
    }

    public double durationSeconds(Path video) {
        try {
            String output = runCaptured(List.of(properties.getFfprobePath(), "-v", "error",
                    "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1",
                    video.toAbsolutePath().toString()), 30);
            double value = Double.parseDouble(output.trim());
            return value > 0 ? value : 60.0;
        } catch (Exception ex) {
            return 60.0;
        }
    }

    private void run(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        ProcessResult result = execute(command, timeoutSeconds);
        if (result.exitCode() != 0) {
            throw new BusinessException("媒体处理失败：" + abbreviate(result.output()));
        }
    }

    private String runCaptured(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        ProcessResult result = execute(command, timeoutSeconds);
        if (result.exitCode() != 0) {
            throw new BusinessException("媒体信息读取失败：" + abbreviate(result.output()));
        }
        return result.output();
    }

    private ProcessResult execute(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        Path log = Files.createTempFile("media-", ".log");
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.redirectOutput(log.toFile());
            Process process = builder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("媒体处理超时");
            }
            String output = Files.exists(log) ? Files.readString(log) : "";
            return new ProcessResult(process.exitValue(), output);
        } finally {
            try {
                Files.deleteIfExists(log);
            } catch (IOException ignored) {
                // Best-effort cleanup.
            }
        }
    }

    private String abbreviate(String output) {
        String normalized = output == null ? "未知错误" : output.replaceAll("\\s+", " ").trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) + "..." : normalized;
    }

    private record ProcessResult(int exitCode, String output) {
    }
}