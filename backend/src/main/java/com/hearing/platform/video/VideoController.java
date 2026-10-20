package com.hearing.platform.video;

import com.hearing.platform.auth.AuthContext;
import com.hearing.platform.auth.RequireRole;
import com.hearing.platform.auth.Roles;
import com.hearing.platform.common.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequireRole(Roles.STUDENT)
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping
    public ApiResponse<List<VideoDto>> list() {
        return ApiResponse.ok(videoService.list(AuthContext.get()));
    }

    @PostMapping
    public ApiResponse<VideoDto> upload(@RequestParam String title,
                                        @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok("视频已上传", videoService.upload(AuthContext.get(), title, file));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        videoService.deleteOwned(id, AuthContext.get());
        return ApiResponse.ok("视频已删除", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(videoService.detail(id, AuthContext.get()));
    }
}