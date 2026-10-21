package com.hearing.platform.client;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.config.AiProperties;      // AI 配置

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;             // 同步 HTTP 客户端
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * 大模型（LLM）客户端。
 * 对接兼容 OpenAI 格式的 /chat/completions 接口（默认硅基流动），用于文本生成。
 */
@Component
public class LlmClient {

    // AI 配置
    private final AiProperties properties;
    // 复用的 HTTP 客户端（含 baseUrl 与超时设置）
    private final RestClient restClient;

    // 构造器注入配置并构建 RestClient
    public LlmClient(AiProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);   // 连接超时 15s
        factory.setReadTimeout(120_000);     // 读取超时 120s（生成较慢）
        this.restClient = RestClient.builder()
                .baseUrl(properties.getSiliconflow().getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    /**
     * 发送对话请求并返回模型生成的文本。
     *
     * @param systemPrompt 系统提示词（设定角色与约束）
     * @param userPrompt   用户提示词（具体任务与材料）
     * @return 模型回复内容
     */
    public String chat(String systemPrompt, String userPrompt) {
        // 未配置 API Key 直接返回服务不可用
        String key = properties.getSiliconflow().getApiKey();
        if (key == null || key.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "尚未配置 SILICONFLOW_API_KEY");
        }
        // 组装对话请求体：模型、消息（system+user）、较低温度以保证稳定
        Map<String, Object> body = Map.of(
                "model", properties.getSiliconflow().getLlmModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3
        );
        try {
            // POST 并取回 JSON 响应
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + key)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "大模型返回为空");
            }
            // 从 choices[0].message.content 提取文本
            String content = response.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "大模型没有返回有效内容");
            }
            return content.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            // HTTP 错误状态：附带状态码与截断后的响应体
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "调用大模型失败 HTTP " + ex.getStatusCode().value() + "：" + abbreviate(ex.getResponseBodyAsString()));
        } catch (Exception ex) {
            // 其他异常（网络等）统一转为 502
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "调用大模型失败：" + ex.getMessage());
        }
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
