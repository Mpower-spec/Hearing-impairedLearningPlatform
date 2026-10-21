package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.dao.VideoMapper;          // 视频数据访问
import com.hearing.platform.dto.VideoDto;             // 视频展示对象
import com.hearing.platform.entity.Video;             // 视频实体
import com.hearing.platform.security.AuthUser;        // 已认证用户

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 视频业务服务。
 * 负责学生视频的上传、列表、详情、删除，以及供 AI 流程使用的状态更新。
 * 所有操作均以"视频归属当前学生"为权限前提。
 */
@Service
public class VideoService {

    // 视频大小上限：100MB
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;

    // 视频数据访问
    private final VideoMapper videoMapper;
    // 文件存储服务
    private final FileStorageService storageService;

    // 构造器注入依赖
    public VideoService(VideoMapper videoMapper, FileStorageService storageService) {
        this.videoMapper = videoMapper;
        this.storageService = storageService;
    }

    /**
     * 上传视频：存储文件并落库，初始状态为 UPLOADED。
     *
     * @param user  当前学生
     * @param title 视频标题
     * @param file  上传的视频文件
     * @return 视频信息
     */
    @Transactional
    public VideoDto upload(AuthUser user, String title, MultipartFile file) {
        // 标题为空则拒绝
        if (title == null || title.isBlank()) {
            throw new BusinessException("请输入视频标题");
        }
        // 存储到 video 子目录，仅允许 mp4/webm，受大小限制
        String relative = storageService.store(file, "video", Set.of("mp4", "webm"), MAX_VIDEO_BYTES);
        LocalDateTime now = LocalDateTime.now();
        // 组装视频记录
        Video video = new Video();
        video.setUserId(user.id());              // 归属学生
        video.setTitle(title.trim());            // 标题
        video.setVideoPath(relative);            // 相对存储路径
        video.setStatus("UPLOADED");             // 初始状态
        video.setTimingMode("NONE");             // 尚未生成字幕时间轴
        video.setCreatedAt(now);
        video.setUpdatedAt(now);
        videoMapper.insert(video);
        return toDto(video);
    }

    /**
     * 查询当前学生的视频列表（按创建时间倒序）。
     */
    public List<VideoDto> list(AuthUser user) {
        return videoMapper.selectList(new LambdaQueryWrapper<Video>()
                        .eq(Video::getUserId, user.id()).orderByDesc(Video::getCreatedAt))
                .stream().map(this::toDto).toList();
    }

    /**
     * 获取视频并校验归属；不存在返回 404，非本人返回 403。
     *
     * @param videoId 视频 ID
     * @param user    当前用户
     * @return 视频实体
     */
    public Video requireOwned(Long videoId, AuthUser user) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw BusinessException.notFound("视频不存在");
        }
        if (!video.getUserId().equals(user.id())) {
            throw BusinessException.forbidden("无权访问该视频");
        }
        return video;
    }

    /**
     * 删除视频：先校验归属，删除数据库记录并清理磁盘文件。
     */
    @Transactional
    public void deleteOwned(Long videoId, AuthUser user) {
        Video video = requireOwned(videoId, user);
        videoMapper.deleteById(videoId);
        // 清理视频、音频、字幕三类文件（静默失败）
        storageService.deleteQuietly(video.getVideoPath());
        storageService.deleteQuietly(video.getAudioPath());
        storageService.deleteQuietly(video.getVttPath());
    }

    /**
     * 视频详情（含归属校验）。
     */
    public VideoDto detail(Long videoId, AuthUser user) {
        return toDto(requireOwned(videoId, user));
    }

    /**
     * 更新视频记录并刷新更新时间（供 AI 流程写回字幕/笔记/状态）。
     */
    public void update(Video video) {
        video.setUpdatedAt(LocalDateTime.now());
        videoMapper.updateById(video);
    }

    /**
     * 实体转 DTO，并补充视频与字幕文件的可访问 URL。
     */
    public VideoDto toDto(Video video) {
        return VideoDto.from(video, storageService.url(video.getVideoPath()), storageService.url(video.getVttPath()));
    }
}
