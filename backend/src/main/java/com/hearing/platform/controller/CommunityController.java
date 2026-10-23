package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;
import com.hearing.platform.dto.CommunityCommentDto;
import com.hearing.platform.dto.CommunityPostDetailDto;
import com.hearing.platform.dto.CommunityPostDto;
import com.hearing.platform.dto.CommunityPostPageDto;
import com.hearing.platform.dto.CreateCommunityCommentRequest;
import com.hearing.platform.dto.CreateCommunityPostRequest;
import com.hearing.platform.security.AuthContext;
import com.hearing.platform.service.CommunityService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/posts")
    public ApiResponse<CommunityPostPageDto> posts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(communityService.list(AuthContext.get(), category, keyword, page, size));
    }

    @PostMapping("/posts")
    public ApiResponse<CommunityPostDto> createPost(@Valid @RequestBody CreateCommunityPostRequest request) {
        return ApiResponse.ok("帖子已发布", communityService.create(AuthContext.get(), request));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<CommunityPostDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(communityService.detail(id, AuthContext.get()));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        communityService.deletePost(id, AuthContext.get());
        return ApiResponse.ok("帖子已删除", null);
    }

    @PostMapping("/posts/{id}/comments")
    public ApiResponse<CommunityCommentDto> addComment(@PathVariable Long id,
                                                       @Valid @RequestBody CreateCommunityCommentRequest request) {
        return ApiResponse.ok("评论已发布", communityService.addComment(id, AuthContext.get(), request));
    }

    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        communityService.deleteComment(id, AuthContext.get());
        return ApiResponse.ok("评论已删除", null);
    }
}
