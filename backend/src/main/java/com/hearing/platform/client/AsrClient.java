package com.hearing.platform.client;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.config.AiProperties;      // AI 配置

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;      // JDK 原生 HTTP 客户端
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * 语音识别（ASR）客户端。
 * 对接 /audio/transcriptions 接口（默认硅基流动 SenseVoice），
 * 手动组装 multipart/form-data 上传音频并解析返回文本。
 */
@Component
public class AsrClient {

    // AI 配置
    private final AiProperties properties;
    // JSON 解析器
    private final ObjectMapper objectMapper;
    // HTTP 客户端（连接超时 15s）
    private final HttpClient httpClient;

    // 构造器注入依赖并初始化 HttpClient
    public AsrClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * 转写音频文件为文本。
     *
     * @param audioFile 音频文件路径
     * @return 识别出的文本
     */
    public String transcribe(Path audioFile) {
        // 未配置 API Key 直接返回服务不可用
        String key = properties.getSiliconflow().getApiKey();
        if (key == null || key.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "尚未配置 SILICONFLOW_API_KEY");
        }
        try {
            // 读取音频字节并校验大小（接口限制 50MB）
            byte[] audio = Files.readAllBytes(audioFile);
            if (audio.length > 50 * 1024 * 1024) {
                throw new BusinessException("提取后的音频超过硅基流动 50MB 限制");
            }
            // 生成分隔符并构造 multipart 请求体
            String boundary = "----HearingPlatform" + UUID.randomUUID();
            byte[] body = buildMultipart(boundary, properties.getSiliconflow().getAsrModel(),
                    audioFile.getFileName().toString(), audio);

            // 拼接转写端点（去掉 baseUrl 末尾斜杠）
            String endpoint = properties.getSiliconflow().getBaseUrl().replaceAll("/$", "") + "/audio/transcriptions";
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMinutes(5))                       // 整体请求超时 5 分钟
                    .header("Authorization", "Bearer " + key)             // 鉴权头
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary) // multipart 类型
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))    // POST 二进制体
                    .build();
            // 发送请求，按 UTF-8 读取文本响应
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            // 非 2xx 视为失败
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY,
                        "语音识别失败 HTTP " + response.statusCode() + "：" + abbreviate(response.body()));
            }
            // 从返回 JSON 的 text 字段取识别文本
            JsonNode json = objectMapper.readTree(response.body());
            String text = json.path("text").asText("");
            if (text.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "语音识别没有返回文本");
            }
            return text.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            // 读文件失败或响应解析失败
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "读取音频或解析响应失败：" + ex.getMessage());
        } catch (InterruptedException ex) {
            // 恢复中断标志并报错
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "语音识别请求被中断");
        } catch (Exception ex) {
            // 其他异常统一转为 502
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "调用语音识别失败：" + ex.getMessage());
        }
    }

    /**
     * 手动组装 multipart/form-data 请求体。
     * 依次写入 model 字段、file 字段（含文件名与类型）及音频二进制，最后补结束分隔符。
     */
    private byte[] buildMultipart(String boundary, String model, String filename, byte[] audio) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(audio.length + 2048);
        // 段 1：model
        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        write(output, model + "\r\n");
        // 段 2：file（音频）
        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n");
        write(output, "Content-Type: audio/mpeg\r\n\r\n");
        output.write(audio);
        // 结束分隔符（以 --boundary-- 标记 multipart 结束）
        write(output, "\r\n--" + boundary + "--\r\n");
        return output.toByteArray();
    }

    // 以 UTF-8 将字符串段写入输出流
    private void write(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    // 压缩并截断响应体到 500 字符，便于放入错误消息
    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "无响应内容";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) + "..." : normalized;
    }
}
