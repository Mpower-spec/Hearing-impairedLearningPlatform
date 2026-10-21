package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.config.AppProperties;     // 应用配置（含上传根目录）

import jakarta.annotation.PostConstruct;              // 初始化回调
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务。
 * 统一管理上传根目录下的各类子目录（pptx/pdf/video/audio/vtt），
 * 提供安全的路径解析（防目录穿越）、文件存储、文本写入、URL 生成与清理。
 */
@Service
public class FileStorageService {

    // 需要预创建的子目录
    private static final List<String> DIRECTORIES = List.of("pptx", "pdf", "video", "audio", "vtt");

    // 应用配置
    private final AppProperties properties;
    // 存储根目录的绝对路径（初始化时确定）
    private Path root;

    // 构造器注入配置
    public FileStorageService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * 启动后初始化：解析并创建存储根目录及各子目录。
     */
    @PostConstruct
    public void initialize() throws IOException {
        // 将配置的相对路径转为规范化绝对路径
        root = Path.of(properties.getUploadRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        // 确保每个子目录存在
        for (String directory : DIRECTORIES) {
            Files.createDirectories(root.resolve(directory));
        }
    }

    /**
     * 存储上传文件。
     * 校验非空、大小、扩展名后，以 UUID 命名写入指定子目录。
     *
     * @param file             上传文件
     * @param subdirectory     目标子目录（如 video）
     * @param allowedExtensions 允许的扩展名集合
     * @param maxBytes         大小上限（字节）
     * @return 相对存储根的路径（统一 / 分隔）
     */
    public String store(MultipartFile file, String subdirectory, Set<String> allowedExtensions, long maxBytes) {
        // 空文件校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        // 大小校验
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件过大，最大允许 " + (maxBytes / 1024 / 1024) + "MB");
        }
        // 提取并校验扩展名
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException("不支持的文件类型，仅允许：" + String.join(", ", allowedExtensions));
        }
        // 以 UUID 生成唯一相对路径，避免文件名冲突
        String relative = subdirectory + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            // 将上传流拷贝到目标位置（覆盖同名）
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return relative;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "保存文件失败：" + ex.getMessage());
        }
    }

    /**
     * 解析并校验 VTT 字幕文件路径。
     * 仅允许纯文件名（拒绝路径分隔符与 ..），且文件必须存在。
     *
     * @param filename 字幕文件名
     * @return 字幕文件绝对路径
     */
    public Path resolveVtt(String filename) {
        // 合法性校验：非空且不含路径穿越字符
        if (filename == null || filename.isBlank() || filename.contains("..")
                || filename.contains("/") || filename.contains("\\")) {
            throw BusinessException.notFound("字幕文件不存在");
        }
        Path path = resolve("vtt/" + filename);
        // 文件不存在同样按 404 处理
        if (!Files.exists(path)) {
            throw BusinessException.notFound("字幕文件不存在");
        }
        return path;
    }

    /**
     * 将相对路径安全解析为绝对路径。
     * 归一化后必须仍位于存储根之内，否则视为非法路径穿越。
     */
    public Path resolve(String relative) {
        Path candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root)) {
            throw new BusinessException("非法文件路径");
        }
        return candidate;
    }

    /**
     * 将文本内容（如 VTT 字幕）以 UUID 命名写入指定子目录。
     *
     * @param subdirectory 目标子目录
     * @param content      文本内容
     * @return 相对存储根的路径
     */
    public String writeText(String subdirectory, String content) {
        String relative = subdirectory + "/" + UUID.randomUUID() + ".vtt";
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            // 以 UTF-8 写入
            Files.writeString(target, content, java.nio.charset.StandardCharsets.UTF_8);
            return relative;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "保存字幕文件失败：" + ex.getMessage());
        }
    }

    // 返回存储根目录绝对路径
    public Path root() {
        return root;
    }

    // 将相对路径转为对外可访问的 /files/ URL（统一 / 分隔）
    public String url(String relative) {
        return relative == null ? null : "/files/" + relative.replace('\\', '/');
    }

    // 静默删除文件：忽略任何异常，避免清理失败掩盖主操作结果
    public void deleteQuietly(String relative) {
        if (relative == null || relative.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relative));
        } catch (Exception ignored) {
            // A failed cleanup must not hide the original operation result.
        }
    }

    // 提取小写扩展名（无扩展名返回空串）
    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
