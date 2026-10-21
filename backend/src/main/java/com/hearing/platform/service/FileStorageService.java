package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException;
import com.hearing.platform.config.AppProperties;

import jakarta.annotation.PostConstruct;
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

@Service
public class FileStorageService {

    private static final List<String> DIRECTORIES = List.of("pptx", "pdf", "video", "audio", "vtt");

    private final AppProperties properties;
    private Path root;

    public FileStorageService(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() throws IOException {
        root = Path.of(properties.getUploadRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        for (String directory : DIRECTORIES) {
            Files.createDirectories(root.resolve(directory));
        }
    }

    public String store(MultipartFile file, String subdirectory, Set<String> allowedExtensions, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件过大，最大允许 " + (maxBytes / 1024 / 1024) + "MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException("不支持的文件类型，仅允许：" + String.join(", ", allowedExtensions));
        }
        String relative = subdirectory + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return relative;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "保存文件失败：" + ex.getMessage());
        }
    }

    public Path resolveVtt(String filename) {
        if (filename == null || filename.isBlank() || filename.contains("..")
                || filename.contains("/") || filename.contains("\\")) {
            throw BusinessException.notFound("字幕文件不存在");
        }
        Path path = resolve("vtt/" + filename);
        if (!Files.exists(path)) {
            throw BusinessException.notFound("字幕文件不存在");
        }
        return path;
    }

    public Path resolve(String relative) {
        Path candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root)) {
            throw new BusinessException("非法文件路径");
        }
        return candidate;
    }

    public String writeText(String subdirectory, String content) {
        String relative = subdirectory + "/" + UUID.randomUUID() + ".vtt";
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, java.nio.charset.StandardCharsets.UTF_8);
            return relative;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "保存字幕文件失败：" + ex.getMessage());
        }
    }

    public Path root() {
        return root;
    }

    public String url(String relative) {
        return relative == null ? null : "/files/" + relative.replace('\\', '/');
    }

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

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}