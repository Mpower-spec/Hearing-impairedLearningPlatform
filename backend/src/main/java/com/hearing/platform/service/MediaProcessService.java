package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.config.AppProperties;     // 应用配置（FFmpeg/ffprobe 路径）

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 媒体处理服务。
 * 通过外部 FFmpeg/ffprobe 完成"视频抽取音频"与"读取视频时长"，
 * 统一封装进程执行、超时控制、日志收集与错误处理。
 */
@Service
public class MediaProcessService {

    // 应用配置
    private final AppProperties properties;

    // 构造器注入配置
    public MediaProcessService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * 从视频抽取音频为 MP3。
     * 采用单声道、16kHz、64kbps，契合 ASR 输入要求。
     *
     * @param video           视频文件路径
     * @param outputDirectory 输出目录
     * @param baseName        输出文件基础名（不含扩展名）
     * @return 生成的音频文件路径
     */
    public Path extractAudio(Path video, Path outputDirectory, String baseName) {
        try {
            Files.createDirectories(outputDirectory);
            Path output = outputDirectory.resolve(baseName + ".mp3");
            // -vn 去视频流；-ac 1 单声道；-ar 16000 采样率；libmp3lame 编码器；64k 码率
            run(List.of(properties.getFfmpegPath(), "-y", "-i", video.toAbsolutePath().toString(),
                    "-vn", "-ac", "1", "-ar", "16000", "-c:a", "libmp3lame", "-b:a", "64k", output.toAbsolutePath().toString()), 240);
            // 校验产物存在且非空
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

    /**
     * 读取视频时长（秒）。
     * 任何异常或非法值都回退为默认 60 秒，保证字幕估算流程不中断。
     */
    public double durationSeconds(Path video) {
        try {
            // ffprobe 仅取 duration 字段，无键裸值输出
            String output = runCaptured(List.of(properties.getFfprobePath(), "-v", "error",
                    "-show_entries", "format=duration", "-of", "default=noprint_wrappers=1:nokey=1",
                    video.toAbsolutePath().toString()), 30);
            double value = Double.parseDouble(output.trim());
            // 非正时长回退为 60 秒
            return value > 0 ? value : 60.0;
        } catch (Exception ex) {
            return 60.0;
        }
    }

    // 执行命令，退出码非 0 视为失败
    private void run(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        ProcessResult result = execute(command, timeoutSeconds);
        if (result.exitCode() != 0) {
            throw new BusinessException("媒体处理失败：" + abbreviate(result.output()));
        }
    }

    // 执行命令并返回其输出，退出码非 0 视为失败
    private String runCaptured(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        ProcessResult result = execute(command, timeoutSeconds);
        if (result.exitCode() != 0) {
            throw new BusinessException("媒体信息读取失败：" + abbreviate(result.output()));
        }
        return result.output();
    }

    /**
     * 统一进程执行：输出重定向到临时日志，带超时控制，返回退出码与输出，最后清理日志。
     */
    private ProcessResult execute(List<String> command, long timeoutSeconds) throws IOException, InterruptedException {
        Path log = Files.createTempFile("media-", ".log");
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);        // 合并错误流
            builder.redirectOutput(log.toFile());     // 输出到日志
            Process process = builder.start();
            // 超时则强制终止并报错
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("媒体处理超时");
            }
            String output = Files.exists(log) ? Files.readString(log) : "";
            return new ProcessResult(process.exitValue(), output);
        } finally {
            // 尽力清理临时日志
            try {
                Files.deleteIfExists(log);
            } catch (IOException ignored) {
                // Best-effort cleanup.
            }
        }
    }

    // 压缩并截断输出到 300 字符，便于放入错误消息
    private String abbreviate(String output) {
        String normalized = output == null ? "未知错误" : output.replaceAll("\\s+", " ").trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) + "..." : normalized;
    }

    // 进程执行结果：退出码 + 输出文本
    private record ProcessResult(int exitCode, String output) {
    }
}
