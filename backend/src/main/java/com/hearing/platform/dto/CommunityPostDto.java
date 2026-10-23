package com.hearing.platform.dto;

import com.hearing.platform.entity.AppUser;
import com.hearing.platform.entity.CommunityPost;

import java.time.LocalDateTime;

public record CommunityPostDto(
        Long id,
        String category,
        String title,
        String content,
        Long authorId,
        String authorName,
        String authorRole,
        long commentCount,
        boolean canDelete,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommunityPostDto from(CommunityPost post, AppUser author, long commentCount, Long currentUserId) {
        String displayName = author == null ? "已注销用户"
                : (author.getName() == null || author.getName().isBlank() ? author.getUsername() : author.getName());
        String role = author == null ? "STUDENT" : author.getRole();
        return new CommunityPostDto(post.getId(), post.getCategory(), post.getTitle(), post.getContent(),
                post.getUserId(), displayName, role, commentCount,
                post.getUserId().equals(currentUserId), post.getCreatedAt(), post.getUpdatedAt());
    }
}
