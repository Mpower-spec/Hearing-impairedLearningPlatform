package com.hearing.platform.dto;

import com.hearing.platform.entity.AppUser;
import com.hearing.platform.entity.CommunityComment;

import java.time.LocalDateTime;

public record CommunityCommentDto(
        Long id,
        Long postId,
        String content,
        Long authorId,
        String authorName,
        String authorRole,
        boolean canDelete,
        LocalDateTime createdAt
) {
    public static CommunityCommentDto from(CommunityComment comment, AppUser author, Long currentUserId) {
        String displayName = author == null ? "已注销用户"
                : (author.getName() == null || author.getName().isBlank() ? author.getUsername() : author.getName());
        String role = author == null ? "STUDENT" : author.getRole();
        return new CommunityCommentDto(comment.getId(), comment.getPostId(), comment.getContent(),
                comment.getUserId(), displayName, role,
                comment.getUserId().equals(currentUserId), comment.getCreatedAt());
    }
}
