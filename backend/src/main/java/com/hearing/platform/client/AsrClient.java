package com.hearing.platform.client;

import com.hearing.platform.common.BusinessException;
import com.hearing.platform.config.AiProperties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

@Component
public class AsrClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AsrClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public String transcribe(Path audioFile) {
        String key = properties.getSiliconflow().getApiKey();
        if (key == null || key.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "尚未配置 SILICONFLOW_API_KEY");
        }
        try {
            byte[] audio = Files.readAllBytes(audioFile);
            if (audio.length > 50 * 1024 * 1024) {
                throw new BusinessException("提取后的音频超过硅基流动 50MB 限制");
            }
            String boundary = "----HearingPlatform" + UUID.randomUUID();
            byte[] body = buildMultipart(boundary, properties.getSiliconflow().getAsrModel(),
                    audioFile.getFileName().toString(), audio);

            String endpoint = properties.getSiliconflow().getBaseUrl().replaceAll("/$", "") + "/audio/transcriptions";
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofMinutes(5))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY,
                        "语音识别失败 HTTP " + response.statusCode() + "：" + abbreviate(response.body()));
            }
            JsonNode json = objectMapper.readTree(response.body());
            String text = json.path("text").asText("");
            if (text.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "语音识别没有返回文本");
            }
            return text.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "读取音频或解析响应失败：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "语音识别请求被中断");
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "调用语音识别失败：" + ex.getMessage());
        }
    }

    private byte[] buildMultipart(String boundary, String model, String filename, byte[] audio) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(audio.length + 2048);
        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        write(output, model + "\r\n");
        write(output, "--" + boundary + "\r\n");
        write(output, "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n");
        write(output, "Content-Type: audio/mpeg\r\n\r\n");
        output.write(audio);
        write(output, "\r\n--" + boundary + "--\r\n");
        return output.toByteArray();
    }

    private void write(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "无响应内容";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) + "..." : normalized;
    }
}