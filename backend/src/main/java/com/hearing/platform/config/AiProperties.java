package com.hearing.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties; // 配置属性绑定注解

/**
 * AI 相关配置属性。
 * 绑定 application.yml 中 ai.* 前缀的配置。
 * 控制调用模式、降级开关及硅基流动对接参数。
 */
@ConfigurationProperties(prefix = "ai") // 映射 yml 中的 ai 前缀
public class AiProperties {

    private String mode = "auto"; // 调用模式：auto/real/mock，默认 auto
    private boolean fallbackEnabled = true; // 调用失败是否回退 Mock，默认开启
    private final SiliconFlow siliconflow = new SiliconFlow(); // 硅基流动子配置

    public String getMode() { return mode; } // 读取调用模式
    public void setMode(String mode) { this.mode = mode; } // 写入调用模式
    public boolean isFallbackEnabled() { return fallbackEnabled; } // 读取降级开关
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; } // 写入降级开关
    public SiliconFlow getSiliconflow() { return siliconflow; } // 读取硅基流动子配置

    /**
     * 硅基流动平台配置。
     * 对应 yml 中 ai.siliconflow.* 的配置项。
     */
    public static class SiliconFlow {
        private String baseUrl = "https://api.siliconflow.cn/v1"; // 接口基础地址
        private String apiKey; // 密钥（无默认，须外部配置）
        private String llmModel = "Qwen/Qwen2.5-72B-Instruct"; // 大语言模型名称
        private String asrModel = "FunAudioLLM/SenseVoiceSmall"; // 语音识别模型名称

        public String getBaseUrl() { return baseUrl; } // 读取基础地址
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; } // 写入基础地址
        public String getApiKey() { return apiKey; } // 读取密钥
        public void setApiKey(String apiKey) { this.apiKey = apiKey; } // 写入密钥
        public String getLlmModel() { return llmModel; } // 读取 LLM 模型名
        public void setLlmModel(String llmModel) { this.llmModel = llmModel; } // 写入 LLM 模型名
        public String getAsrModel() { return asrModel; } // 读取 ASR 模型名
        public void setAsrModel(String asrModel) { this.asrModel = asrModel; } // 写入 ASR 模型名
    }
}
