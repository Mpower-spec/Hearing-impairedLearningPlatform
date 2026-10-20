package com.hearing.platform.video;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hearing.platform.auth.AuthUser;
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class VideoService {

    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;

    private final VideoMapper videoMapper;
    private final FileStorageService storageService;

    public VideoService(VideoMapper videoMapper, FileStorageService storageService) {
        this.videoMapper = videoMapper;
        this.storageService = storageService;
    }

    @Transactional
    public VideoDto upload(AuthUser user, String title, MultipartFile file) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("请输入视频标题");
        }
        String relative = storageService.store(file, "video", Set.of("mp4", "webm"), MAX_VIDEO_BYTES);
        LocalDateTime now = LocalDateTime.now();
        Video video = new Video();
        video.setUserId(user.id());
        video.setTitle(title.trim());
        video.setVideoPath(relative);
        video.setStatus("UPLOADED");
        video.setTimingMode("NONE");
        video.setCreatedAt(now);
        video.setUpdatedAt(now);
        videoMapper.insert(video);
        return toDto(video);
    }

    public List<VideoDto> list(AuthUser user) {
        return videoMapper.selectList(new LambdaQueryWrapper<Video>()
                        .eq(Video::getUserId, user.id()).orderByDesc(Video::getCreatedAt))
                .stream().map(this::toDto).toList();
    }

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

    @Transactional
    public void deleteOwned(Long videoId, AuthUser user) {
        Video video = requireOwned(videoId, user);
        videoMapper.deleteById(videoId);
        storageService.deleteQuietly(video.getVideoPath());
        storageService.deleteQuietly(video.getAudioPath());
        storageService.deleteQuietly(video.getVttPath());
    }

    public VideoDto detail(Long videoId, AuthUser user) {
        return toDto(requireOwned(videoId, user));
    }

    public void update(Video video) {
        video.setUpdatedAt(LocalDateTime.now());
        videoMapper.updateById(video);
    }

    public VideoDto toDto(Video video) {
        return VideoDto.from(video, storageService.url(video.getVideoPath()), storageService.url(video.getVttPath()));
    }
}