package com.hearing.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.dao.CommunityCommentMapper;
import com.hearing.platform.dao.CommunityPostMapper;
import com.hearing.platform.dao.UserMapper;
import com.hearing.platform.dto.CommunityCommentDto;
import com.hearing.platform.dto.CommunityPostDetailDto;
import com.hearing.platform.dto.CommunityPostDto;
import com.hearing.platform.dto.CommunityPostPageDto;
import com.hearing.platform.dto.CreateCommunityCommentRequest;
import com.hearing.platform.dto.CreateCommunityPostRequest;
import com.hearing.platform.entity.AppUser;
import com.hearing.platform.entity.CommunityComment;
import com.hearing.platform.entity.CommunityPost;
import com.hearing.platform.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CommunityService {

    private static final Set<String> CATEGORIES = Set.of("学习讨论", "提问求助", "经验分享", "课程交流");
    private static final int MAX_PAGE_SIZE = 30;

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final UserMapper userMapper;

    public CommunityService(CommunityPostMapper postMapper, CommunityCommentMapper commentMapper,
                            UserMapper userMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
    }

    public CommunityPostPageDto list(AuthUser user, String category, String keyword, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            validateCategory(category);
            wrapper.eq(CommunityPost::getCategory, category.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            wrapper.and(query -> query.like(CommunityPost::getTitle, value)
                    .or().like(CommunityPost::getContent, value));
        }
        wrapper.orderByDesc(CommunityPost::getCreatedAt);
        long total = postMapper.selectCount(wrapper);
        long offset = (long) (safePage - 1) * safeSize;
        List<CommunityPost> posts = postMapper.selectList(wrapper
                .last("LIMIT " + safeSize + " OFFSET " + offset));
        List<CommunityPostDto> items = toPostDtos(posts, user.id());
        long totalPages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        return new CommunityPostPageDto(items, total, safePage, safeSize, totalPages);
    }

    @Transactional
    public CommunityPostDto create(AuthUser user, CreateCommunityPostRequest request) {
        String category = request.category().trim();
        validateCategory(category);
        LocalDateTime now = LocalDateTime.now();
        CommunityPost post = new CommunityPost();
        post.setUserId(user.id());
        post.setCategory(category);
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        postMapper.insert(post);
        AppUser author = userMapper.selectById(user.id());
        return CommunityPostDto.from(post, author, 0, user.id());
    }

    public CommunityPostDetailDto detail(Long postId, AuthUser user) {
        CommunityPost post = requirePost(postId);
        AppUser author = userMapper.selectById(post.getUserId());
        List<CommunityComment> comments = commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getPostId, postId)
                .orderByAsc(CommunityComment::getCreatedAt));
        Map<Long, AppUser> authors = loadUsers(comments.stream().map(CommunityComment::getUserId).toList());
        List<CommunityCommentDto> commentDtos = comments.stream()
                .map(comment -> CommunityCommentDto.from(comment, authors.get(comment.getUserId()), user.id()))
                .toList();
        return new CommunityPostDetailDto(
                CommunityPostDto.from(post, author, comments.size(), user.id()),
                commentDtos);
    }

    @Transactional
    public CommunityCommentDto addComment(Long postId, AuthUser user, CreateCommunityCommentRequest request) {
        requirePost(postId);
        CommunityComment comment = new CommunityComment();
        comment.setPostId(postId);
        comment.setUserId(user.id());
        comment.setContent(request.content().trim());
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return CommunityCommentDto.from(comment, userMapper.selectById(user.id()), user.id());
    }

    @Transactional
    public void deletePost(Long postId, AuthUser user) {
        CommunityPost post = requirePost(postId);
        if (!post.getUserId().equals(user.id())) {
            throw BusinessException.forbidden("只能删除自己发布的帖子");
        }
        commentMapper.delete(new LambdaQueryWrapper<CommunityComment>().eq(CommunityComment::getPostId, postId));
        postMapper.deleteById(postId);
    }

    @Transactional
    public void deleteComment(Long commentId, AuthUser user) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw BusinessException.notFound("评论不存在");
        }
        if (!comment.getUserId().equals(user.id())) {
            throw BusinessException.forbidden("只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
    }

    private List<CommunityPostDto> toPostDtos(List<CommunityPost> posts, Long currentUserId) {
        if (posts.isEmpty()) return List.of();
        List<Long> postIds = posts.stream().map(CommunityPost::getId).toList();
        Map<Long, AppUser> authors = loadUsers(posts.stream().map(CommunityPost::getUserId).toList());
        Map<Long, Long> commentCounts = new HashMap<>();
        commentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                        .in(CommunityComment::getPostId, postIds)
                        .select(CommunityComment::getPostId))
                .forEach(comment -> commentCounts.merge(comment.getPostId(), 1L, Long::sum));
        return posts.stream()
                .map(post -> CommunityPostDto.from(post, authors.get(post.getUserId()),
                        commentCounts.getOrDefault(post.getId(), 0L), currentUserId))
                .toList();
    }

    private Map<Long, AppUser> loadUsers(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(userIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
    }

    private CommunityPost requirePost(Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw BusinessException.notFound("帖子不存在");
        return post;
    }

    private void validateCategory(String category) {
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException("不支持的讨论分类");
        }
    }
}
