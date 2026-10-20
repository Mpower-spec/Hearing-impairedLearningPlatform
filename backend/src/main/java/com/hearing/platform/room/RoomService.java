package com.hearing.platform.room;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hearing.platform.auth.AuthUser;
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.lesson.Lesson;
import com.hearing.platform.lesson.LessonMapper;
import com.hearing.platform.lesson.LessonService;
import com.hearing.platform.storage.FileStorageService;
import com.hearing.platform.user.AppUser;
import com.hearing.platform.user.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomMapper roomMapper;
    private final RoomMemberMapper memberMapper;
    private final CaptionMapper captionMapper;
    private final DanmakuMapper danmakuMapper;
    private final LessonMapper lessonMapper;
    private final LessonService lessonService;
    private final UserMapper userMapper;
    private final FileStorageService storageService;

    public RoomService(RoomMapper roomMapper, RoomMemberMapper memberMapper, CaptionMapper captionMapper,
                       DanmakuMapper danmakuMapper, LessonMapper lessonMapper, LessonService lessonService,
                       UserMapper userMapper, FileStorageService storageService) {
        this.roomMapper = roomMapper;
        this.memberMapper = memberMapper;
        this.captionMapper = captionMapper;
        this.danmakuMapper = danmakuMapper;
        this.lessonMapper = lessonMapper;
        this.lessonService = lessonService;
        this.userMapper = userMapper;
        this.storageService = storageService;
    }

    @Transactional
    public RoomDto create(AuthUser teacher, Long lessonId) {
        Lesson lesson = lessonService.requireOwned(lessonId, teacher);
        if (lesson.getPdfPath() == null || lesson.getPdfPath().isBlank()) {
            throw new BusinessException("课程尚未完成 PPT 转换");
        }
        closeLiveRoomsForLesson(lessonId);
        Room room = new Room();
        room.setLessonId(lessonId);
        room.setRoomCode(generateCode());
        room.setCurrentPage(1);
        room.setStatus("LIVE");
        room.setCreatedAt(LocalDateTime.now());
        roomMapper.insert(room);
        return toDto(room, lesson);
    }

    public List<RoomDto> listForTeacher(AuthUser teacher) {
        List<Lesson> lessons = lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getTeacherId, teacher.id()));
        if (lessons.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Lesson> lessonMap = lessons.stream()
                .collect(Collectors.toMap(Lesson::getId, Function.identity()));
        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                        .in(Room::getLessonId, lessonMap.keySet())
                        .orderByDesc(Room::getCreatedAt))
                .stream()
                .map(room -> toDto(room, lessonMap.get(room.getLessonId())))
                .toList();
    }

    public RoomDto getForUser(Long roomId, AuthUser user) {
        Room room = requireRoom(roomId);
        Lesson lesson = lessonMapper.selectById(room.getLessonId());
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (user.isTeacher()) {
            if (!lesson.getTeacherId().equals(user.id())) {
                throw BusinessException.forbidden("无权访问该课堂");
            }
        } else if (!isMember(roomId, user.id())) {
            throw BusinessException.forbidden("请先加入该课堂");
        }
        return toDto(room, lesson);
    }

    public RoomDto previewByCode(String roomCode, AuthUser user) {
        Room room = roomMapper.selectOne(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, roomCode));
        if (room == null) {
            throw BusinessException.notFound("房间码不存在");
        }
        Lesson lesson = lessonMapper.selectById(room.getLessonId());
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (user.isTeacher() && !lesson.getTeacherId().equals(user.id())) {
            throw BusinessException.forbidden("该房间属于其他教师");
        }
        return toDto(room, lesson);
    }

    @Transactional
    public Room joinByCode(String roomCode, AuthUser user) {
        Room room = roomMapper.selectOne(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, roomCode));
        if (room == null) {
            throw BusinessException.notFound("房间码不存在");
        }
        if (!"LIVE".equals(room.getStatus())) {
            throw new BusinessException("课堂已经结束");
        }
        Lesson lesson = lessonMapper.selectById(room.getLessonId());
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (user.isTeacher() && !lesson.getTeacherId().equals(user.id())) {
            throw BusinessException.forbidden("该房间属于其他教师");
        }
        if (user.isStudent()) {
            memberMapper.insertIgnore(room.getId(), user.id(), LocalDateTime.now());
        }
        return room;
    }

    public Room requireLive(Long roomId) {
        Room room = requireRoom(roomId);
        if (!"LIVE".equals(room.getStatus())) {
            throw new BusinessException("课堂已经结束");
        }
        return room;
    }

    public Room requireRoom(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw BusinessException.notFound("课堂不存在");
        }
        return room;
    }

    public Lesson requireLessonForRoom(Room room) {
        Lesson lesson = lessonMapper.selectById(room.getLessonId());
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        return lesson;
    }

    public boolean isTeacherOwner(Room room, AuthUser user) {
        Lesson lesson = requireLessonForRoom(room);
        return user.isTeacher() && lesson.getTeacherId().equals(user.id());
    }

    public void updatePage(Long roomId, int page) {
        Room room = requireLive(roomId);
        Lesson lesson = requireLessonForRoom(room);
        if (page < 1 || page > lesson.getPageCount()) {
            throw new BusinessException("页码超出范围");
        }
        room.setCurrentPage(page);
        roomMapper.updateById(room);
    }

    public CaptionDto saveCaption(Long roomId, String content) {
        requireLive(roomId);
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank() || normalized.length() > 1000) {
            throw new BusinessException("字幕内容不能为空且不能超过 1000 字");
        }
        Caption caption = new Caption();
        caption.setRoomId(roomId);
        caption.setContent(normalized);
        caption.setCreatedAt(LocalDateTime.now());
        captionMapper.insert(caption);
        return CaptionDto.from(caption);
    }

    public DanmakuDto saveDanmaku(Long roomId, AuthUser user, String content) {
        requireLive(roomId);
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank() || normalized.length() > 500) {
            throw new BusinessException("弹幕不能为空且不能超过 500 字");
        }
        Danmaku danmaku = new Danmaku();
        danmaku.setRoomId(roomId);
        danmaku.setUserId(user.id());
        danmaku.setContent(normalized);
        danmaku.setCreatedAt(LocalDateTime.now());
        danmakuMapper.insert(danmaku);
        return new DanmakuDto(danmaku.getId(), normalized,
                user.name() == null ? user.username() : user.name(), danmaku.getCreatedAt());
    }

    public List<CaptionDto> captions(Long roomId, AuthUser user) {
        getForUser(roomId, user);
        return captionMapper.selectList(new LambdaQueryWrapper<Caption>()
                        .eq(Caption::getRoomId, roomId).orderByAsc(Caption::getCreatedAt))
                .stream().map(CaptionDto::from).toList();
    }

    public List<DanmakuDto> danmaku(Long roomId, AuthUser user) {
        getForUser(roomId, user);
        List<Danmaku> items = danmakuMapper.selectList(new LambdaQueryWrapper<Danmaku>()
                .eq(Danmaku::getRoomId, roomId).orderByAsc(Danmaku::getCreatedAt));
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, AppUser> users = userMapper.selectBatchIds(items.stream().map(Danmaku::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return items.stream().map(item -> {
            AppUser sender = users.get(item.getUserId());
            return new DanmakuDto(item.getId(), item.getContent(),
                    sender == null ? "未知用户" : (sender.getName() == null ? sender.getUsername() : sender.getName()),
                    item.getCreatedAt());
        }).toList();
    }

    @Transactional
    public void end(Long roomId, AuthUser teacher) {
        Room room = requireRoom(roomId);
        if (!isTeacherOwner(room, teacher)) {
            throw BusinessException.forbidden("只有任课教师可以结束课堂");
        }
        if ("ENDED".equals(room.getStatus())) {
            return;
        }
        room.setStatus("ENDED");
        room.setEndedAt(LocalDateTime.now());
        roomMapper.updateById(room);
    }

    public void requireRoomAccess(Long roomId, AuthUser user) {
        getForUser(roomId, user);
    }

    public boolean isMember(Long roomId, Long userId) {
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<RoomMember>()
                .eq(RoomMember::getRoomId, roomId).eq(RoomMember::getUserId, userId));
        return count != null && count > 0;
    }

    public boolean isMemberOfLesson(Long lessonId, Long userId) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getLessonId, lessonId));
        if (rooms.isEmpty()) {
            return false;
        }
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<RoomMember>()
                .in(RoomMember::getRoomId, rooms.stream().map(Room::getId).toList())
                .eq(RoomMember::getUserId, userId));
        return count != null && count > 0;
    }

    public boolean hasEndedRoomForLesson(Long lessonId, Long userId) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getLessonId, lessonId)
                .eq(Room::getStatus, "ENDED"));
        if (rooms.isEmpty()) {
            return false;
        }
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<RoomMember>()
                .in(RoomMember::getRoomId, rooms.stream().map(Room::getId).toList())
                .eq(RoomMember::getUserId, userId));
        return count != null && count > 0;
    }

    public List<Caption> allCaptionsForEndedRooms(Long lessonId) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getLessonId, lessonId)
                .eq(Room::getStatus, "ENDED")
                .orderByAsc(Room::getCreatedAt));
        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }
        return captionMapper.selectList(new LambdaQueryWrapper<Caption>()
                .in(Caption::getRoomId, rooms.stream().map(Room::getId).toList())
                .orderByAsc(Caption::getCreatedAt));
    }

    public List<Caption> allCaptionsForLesson(Long lessonId) {
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getLessonId, lessonId).orderByAsc(Room::getCreatedAt));
        if (rooms.isEmpty()) {
            return Collections.emptyList();
        }
        return captionMapper.selectList(new LambdaQueryWrapper<Caption>()
                .in(Caption::getRoomId, rooms.stream().map(Room::getId).toList())
                .orderByAsc(Caption::getCreatedAt));
    }

    private RoomDto toDto(Room room, Lesson lesson) {
        return new RoomDto(room.getId(), room.getLessonId(), lesson.getTitle(), room.getRoomCode(),
                room.getCurrentPage(), room.getStatus(), storageService.url(lesson.getPdfPath()),
                lesson.getPageCount(), room.getCreatedAt(), room.getEndedAt());
    }

    private void closeLiveRoomsForLesson(Long lessonId) {
        List<Room> liveRooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getLessonId, lessonId)
                .eq(Room::getStatus, "LIVE"));
        LocalDateTime now = LocalDateTime.now();
        for (Room liveRoom : liveRooms) {
            liveRoom.setStatus("ENDED");
            liveRoom.setEndedAt(now);
            roomMapper.updateById(liveRoom);
        }
    }

    private String generateCode() {
        for (int i = 0; i < 30; i++) {
            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法生成唯一房间码，请重试");
    }
}