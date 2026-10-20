package com.hearing.platform.web;

import com.hearing.platform.common.BusinessException;
import com.hearing.platform.storage.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@RestController
public class StaticContentController {

    private final FileStorageService storageService;

    public StaticContentController(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping(value = "/files/vtt/{filename:.+}", produces = "text/vtt")
    public ResponseEntity<Resource> vtt(@PathVariable String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw BusinessException.notFound("字幕文件不存在");
        }
        Path path = storageService.resolve("vtt/" + filename);
        if (!Files.exists(path)) {
            throw BusinessException.notFound("字幕文件不存在");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/vtt;charset=UTF-8"))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(new FileSystemResource(path));
    }
}