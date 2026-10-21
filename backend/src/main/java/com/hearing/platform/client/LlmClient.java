package com.hearing.platform.client;

import com.hearing.platform.common.BusinessException;
import com.hearing.platform.config.AiProperties;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class LlmClient {

    private final AiProperties properties;
    private final RestClient restClient;

    public LlmClient(AiProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(120_000);
        this.restClient = RestClient.builder()
                .baseUrl(properties.getSiliconflow().getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        String key = properties.getSiliconflow().getApiKey();
        if (key == null || key.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "尚未配置 SILICONFLOW_API_KEY");
        }
        Map<String, Object> body = Map.of(
                "model", properties.getSiliconflow().getLlmModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3
        );
        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + key)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "大模型返回为空");
            }
            String content = response.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "大模型没有返回有效内容");
            }
            return content.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "调用大模型失败 HTTP " + ex.getStatusCode().value() + "：" + abbreviate(ex.getResponseBodyAsString()));
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "调用大模型失败：" + ex.getMessage());
        }
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "无响应内容";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) + "..." : normalized;
    }
}