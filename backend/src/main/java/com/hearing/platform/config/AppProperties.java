package com.hearing.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties; // 配置属性绑定注解

/**
 * 应用级配置属性。
 * 绑定 application.yml 中 app.* 前缀的配置项。
 * 提供上传目录、外部工具路径、JWT 参数等。
 */
@ConfigurationProperties(prefix = "app") // 映射 yml 中的 app 前缀
public class AppProperties {

    private String uploadRoot = "./uploads"; // 上传文件根目录，默认 ./uploads
    private String libreofficePath = "soffice"; // LibreOffice 可执行文件路径（PPT 转 PDF）
    private String ffmpegPath = "ffmpeg"; // FFmpeg 路径（音频抽取）
    private String ffprobePath = "ffprobe"; // FFprobe 路径（读取媒体时长）
    private String jwtSecret; // JWT 签名密钥（无默认，须外部配置）
    private long jwtExpirationHours = 8; // JWT 有效期小时数，默认 8 小时

    public String getUploadRoot() { return uploadRoot; } // 读取上传根目录
    public void setUploadRoot(String uploadRoot) { this.uploadRoot = uploadRoot; } // 写入上传根目录
    public String getLibreofficePath() { return libreofficePath; } // 读取 LibreOffice 路径
    public void setLibreofficePath(String libreofficePath) { this.libreofficePath = libreofficePath; } // 写入 LibreOffice 路径
    public String getFfmpegPath() { return ffmpegPath; } // 读取 FFmpeg 路径
    public void setFfmpegPath(String ffmpegPath) { this.ffmpegPath = ffmpegPath; } // 写入 FFmpeg 路径
    public String getFfprobePath() { return ffprobePath; } // 读取 FFprobe 路径
    public void setFfprobePath(String ffprobePath) { this.ffprobePath = ffprobePath; } // 写入 FFprobe 路径
    public String getJwtSecret() { return jwtSecret; } // 读取 JWT 密钥
    public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; } // 写入 JWT 密钥
    public long getJwtExpirationHours() { return jwtExpirationHours; } // 读取 JWT 有效期
    public void setJwtExpirationHours(long jwtExpirationHours) { this.jwtExpirationHours = jwtExpirationHours; } // 写入 JWT 有效期
}
