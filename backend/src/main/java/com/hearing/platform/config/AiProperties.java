package com.hearing.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String mode = "auto";
    private boolean fallbackEnabled = true;
    private final SiliconFlow siliconflow = new SiliconFlow();

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
    public SiliconFlow getSiliconflow() { return siliconflow; }

    public static class SiliconFlow {
        private String baseUrl = "https://api.siliconflow.cn/v1";
        private String apiKey;
        private String llmModel = "Qwen/Qwen2.5-72B-Instruct";
        private String asrModel = "FunAudioLLM/SenseVoiceSmall";

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getLlmModel() { return llmModel; }
        public void setLlmModel(String llmModel) { this.llmModel = llmModel; }
        public String getAsrModel() { return asrModel; }
        public void setAsrModel(String asrModel) { this.asrModel = asrModel; }
    }
}