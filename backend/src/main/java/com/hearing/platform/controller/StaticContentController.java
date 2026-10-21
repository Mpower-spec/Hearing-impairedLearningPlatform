package com.hearing.platform.controller;

import com.hearing.platform.service.FileStorageService; // 文件存储服务

import org.springframework.core.io.FileSystemResource; // 文件系统资源（作为响应体）
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;          // HTTP 缓存控制
import org.springframework.http.MediaType;             // 响应内容类型
import org.springframework.http.ResponseEntity;        // 原始 HTTP 响应实体
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.time.Duration;

/**
 * 静态内容控制器。
 * 单独提供 VTT 字幕文件的 HTTP 访问，确保以 text/vtt 类型返回并带缓存，
 * 路径解析交由 FileStorageService 做安全校验（防目录穿越）。
 */
@RestController
public class StaticContentController {

    // 文件存储服务，构造器注入
    private final FileStorageService storageService;

    // 构造器注入 FileStorageService
    public StaticContentController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * 返回指定名称的 VTT 字幕文件。
     * 路径变量正则 {.+} 用于保留文件名中的点号（如 xxx.vtt）。
     *
     * @param filename 字幕文件名
     * @return 字幕文件内容，Content-Type 为 text/vtt
     */
    @GetMapping(value = "/files/vtt/{filename:.+}", produces = "text/vtt")
    public ResponseEntity<Resource> vtt(@PathVariable String filename) {
        // 解析并校验字幕文件的实际磁盘路径（非法路径会抛异常）
        Path path = storageService.resolveVtt(filename);
        // 以 UTF-8 的 text/vtt 返回，并设置公共缓存 10 分钟
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/vtt;charset=UTF-8"))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(new FileSystemResource(path));
    }
}
