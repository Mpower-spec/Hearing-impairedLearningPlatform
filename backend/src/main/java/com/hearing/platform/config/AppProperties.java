package com.hearing.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String uploadRoot = "./uploads";
    private String libreofficePath = "soffice";
    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";
    private String jwtSecret;
    private long jwtExpirationHours = 8;

    public String getUploadRoot() { return uploadRoot; }
    public void setUploadRoot(String uploadRoot) { this.uploadRoot = uploadRoot; }
    public String getLibreofficePath() { return libreofficePath; }
    public void setLibreofficePath(String libreofficePath) { this.libreofficePath = libreofficePath; }
    public String getFfmpegPath() { return ffmpegPath; }
    public void setFfmpegPath(String ffmpegPath) { this.ffmpegPath = ffmpegPath; }
    public String getFfprobePath() { return ffprobePath; }
    public void setFfprobePath(String ffprobePath) { this.ffprobePath = ffprobePath; }
    public String getJwtSecret() { return jwtSecret; }
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
    public long getJwtExpirationHours() { return jwtExpirationHours; }
    public void setJwtExpirationHours(long jwtExpirationHours) { this.jwtExpirationHours = jwtExpirationHours; }
}