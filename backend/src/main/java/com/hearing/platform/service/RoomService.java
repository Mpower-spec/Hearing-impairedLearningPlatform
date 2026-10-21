package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.dao.CaptionMapper;        // 字幕数据访问
import com.hearing.platform.dao.DanmakuMapper;        // 弹幕数据访问
import com.hearing.platform.dao.LessonMapper;         // 课程数据访问
import com.hearing.platform.dao.RoomMapper;           // 房间数据访问
import com.hearing.platform.dao.RoomMemberMapper;     // 房间成员数据访问
import com.hearing.platform.dao.UserMapper;           // 用户数据访问
import com.hearing.platform.dto.CaptionDto;
import com.hearing.platform.dto.DanmakuDto;
import com.hearing.platform.dto.RoomDto;
import com.hearing.platform.entity.AppUser;
import com.hearing.platform.entity.Caption;
import com.hearing.platform.entity.Danmaku;
import com.hearing.platform.entity.Lesson;
import com.hearing.platform.entity.Room;
import com.hearing.platform.entity.RoomMember;
import com.hearing.platform.security.AuthUser;        // 已认证用户

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;                    // 安全随机数（生成房间码）
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课堂房间业务服务。
 * 负责课堂的创建、查询、加入、翻页、字幕与弹幕的存取、结束课堂，
 * 并提供多种"成员/权限/字幕聚合"判断供控制器、WebSocket 与 AI 流程复用。
 */
@Service
public class RoomService {

    // 房间码生成使用的安全随机源
    private static final SecureRandom RANDOM = new SecureRandom();

    // 房间数据访问
    private final RoomMapper roomMapper;
    // 成员数据访问
    private final RoomMemberMapper memberMapper;
    // 字幕数据访问
    private final CaptionMapper captionMapper;
    // 弹幕数据访问
    private final DanmakuMapper danmakuMapper;
    // 课程数据访问
    private final LessonMapper lessonMapper;
    // 课程服务（归属校验）
    private final LessonService lessonService;
    // 用户数据访问（弹幕昵称解析）
    private final UserMapper userMapper;
    // 文件存储（生成 PDF 访问 URL）
    private final FileStorageService storageService;

    // 构造器注入依赖
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

    /**
     * 创建课堂房间（仅教师）。
     * 要求课程已完成 PPT 转 PDF；创建前会先结束该课程已有的直播房间，保证同时只有一个 LIVE。
     *
     * @param teacher  当前教师
     * @param lessonId 课程 ID
     * @return 新建房间信息
     */
    @Transactional
    public RoomDto create(AuthUser teacher, Long lessonId) {
        // 校验课程归属该教师
        Lesson lesson = lessonService.requireOwned(lessonId, teacher);
        // 未转 PDF 不能开课
        if (lesson.getPdfPath() == null || lesson.getPdfPath().isBlank()) {
            throw new BusinessException("课程尚未完成 PPT 转换");
        }
        // 关闭同课程已存在的直播房间
        closeLiveRoomsForLesson(lessonId);
        // 组装新房间：生成唯一房间码、初始页为 1、状态 LIVE
        Room room = new Room();
        room.setLessonId(lessonId);
        room.setRoomCode(generateCode());
        room.setCurrentPage(1);
        room.setStatus("LIVE");
        room.setCreatedAt(LocalDateTime.now());
        roomMapper.insert(room);
        return toDto(room, lesson);
    }

    /**
     * 查询当前教师名下所有课程的全部课堂（按创建时间倒序）。
     */
    public List<RoomDto> listForTeacher(AuthUser teacher) {
        // 先取该教师的课程
        List<Lesson> lessons = lessonMapper.selectList(new LambdaQueryWrapper<Lesson>()
                .eq(Lesson::getTeacherId, teacher.id()));
        if (lessons.isEmpty()) {
            return Collections.emptyList();
        }
        // 建立 lessonId -> Lesson 映射，便于组装 DTO 时取标题等信息
        Map<Long, Lesson> lessonMap = lessons.stream()
                .collect(Collectors.toMap(Lesson::getId, Function.identity()));
        // 查询这些课程下的房间并转为 DTO
        return roomMapper.selectList(new LambdaQueryWrapper<Room>()
                        .in(Room::getLessonId, lessonMap.keySet())
                        .orderByDesc(Room::getCreatedAt))
                .stream()
                .map(room -> toDto(room, lessonMap.get(room.getLessonId())))
                .toList();
    }

    /**
     * 获取房间详情并做访问校验：教师需为课程归属者，学生需为房间成员。
     */
    public RoomDto getForUser(Long roomId, AuthUser user) {
        Room room = requireRoom(roomId);
        Lesson lesson = lessonMapper.selectById(room.getLessonId());
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        if (user.isTeacher()) {
            // 教师只能访问自己课程的课堂
            if (!lesson.getTeacherId().equals(user.id())) {
                throw BusinessException.forbidden("无权访问该课堂");
            }
        } else if (!isMember(roomId, user.id())) {
            // 学生必须已加入该课堂
            throw BusinessException.forbidden("请先加入该课堂");
        }
        return toDto(room, lesson);
    }

    /**
     * 按房间码预览课堂信息（加入前的确认）。
     * 教师预览他人房间的课堂会被拒绝。
     */
    public RoomDto previewByCode(String roomCode, AuthUser user) {
        Room room = roomMapper.selectOne(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, roomCode));
        if (room == null) {
            throw BusinessException.notFound("房间码不存在");
        }
        Lesson lesson = lessonMapper.selectById(room.getLessonId());
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        // 教师只能预览属于自己课程的房间
        if (user.isTeacher() && !lesson.getTeacherId().equals(user.id())) {
            throw BusinessException.forbidden("该房间属于其他教师");
        }
        return toDto(room, lesson);
    }

    /**
     * 通过房间码加入课堂。
     * 仅 LIVE 状态可加入；教师需为课程归属者；学生会登记为房间成员（已存在则忽略）。
     *
     * @return 加入的房间实体
     */
    @Transactional
    public Room joinByCode(String roomCode, AuthUser user) {
        Room room = roomMapper.selectOne(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, roomCode));
        if (room == null) {
            throw BusinessException.notFound("房间码不存在");
        }
        // 已结束的房间不可加入
        if (!"LIVE".equals(room.getStatus())) {
            throw new BusinessException("课堂已经结束");
        }
        Lesson lesson = lessonMapper.selectById(room.getLessonId());
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        // 教师只能加入自己课程的房间
        if (user.isTeacher() && !lesson.getTeacherId().equals(user.id())) {
            throw BusinessException.forbidden("该房间属于其他教师");
        }
        // 学生登记成员（INSERT IGNORE 保证幂等）
        if (user.isStudent()) {
            memberMapper.insertIgnore(room.getId(), user.id(), LocalDateTime.now());
        }
        return room;
    }

    /**
     * 获取房间并要求其处于 LIVE 状态，否则视为已结束。
     */
    public Room requireLive(Long roomId) {
        Room room = requireRoom(roomId);
        if (!"LIVE".equals(room.getStatus())) {
            throw new BusinessException("课堂已经结束");
        }
        return room;
    }

    /**
     * 按 ID 获取房间，不存在则 404。
     */
    public Room requireRoom(Long roomId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw BusinessException.notFound("课堂不存在");
        }
        return room;
    }

    /**
     * 获取房间对应课程，不存在则 404。
     */
    public Lesson requireLessonForRoom(Room room) {
        Lesson lesson = lessonMapper.selectById(room.getLessonId());
        if (lesson == null) {
            throw BusinessException.notFound("课程不存在");
        }
        return lesson;
    }

    /**
     * 判断用户是否为该房间所属课程的任课教师。
     */
    public boolean isTeacherOwner(Room room, AuthUser user) {
        Lesson lesson = requireLessonForRoom(room);
        return user.isTeacher() && lesson.getTeacherId().equals(user.id());
    }

    /**
     * 更新课堂当前页（翻页）。要求房间 LIVE 且页码在有效范围内。
     */
    public void updatePage(Long roomId, int page) {
        Room room = requireLive(roomId);
        Lesson lesson = requireLessonForRoom(room);
        // 页码越界校验（1 ~ 总页数）
        if (page < 1 || page > lesson.getPageCount()) {
            throw new BusinessException("页码超出范围");
        }
        room.setCurrentPage(page);
        roomMapper.updateById(room);
    }

    /**
     * 保存课堂字幕。要求房间 LIVE，内容非空且不超过 1000 字。
     */
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

    /**
     * 保存弹幕。要求房间 LIVE，内容非空且不超过 500 字；返回含发送者昵称的 DTO。
     */
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
        // 昵称优先取 name，缺失则回退 username
        return new DanmakuDto(danmaku.getId(), normalized,
                user.name() == null ? user.username() : user.name(), danmaku.getCreatedAt());
    }

    /**
     * 查询房间字幕历史（先做访问校验，按时间升序）。
     */
    public List<CaptionDto> captions(Long roomId, AuthUser user) {
        getForUser(roomId, user);
        return captionMapper.selectList(new LambdaQueryWrapper<Caption>()
                        .eq(Caption::getRoomId, roomId).orderByAsc(Caption::getCreatedAt))
                .stream().map(CaptionDto::from).toList();
    }

    /**
     * 查询房间弹幕历史（先做访问校验，按时间升序）。
     * 批量查出发送者以填充昵称，避免逐条查询。
     */
    public List<DanmakuDto> danmaku(Long roomId, AuthUser user) {
        getForUser(roomId, user);
        List<Danmaku> items = danmakuMapper.selectList(new LambdaQueryWrapper<Danmaku>()
                .eq(Danmaku::getRoomId, roomId).orderByAsc(Danmaku::getCreatedAt));
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        // 批量加载弹幕作者，建立 id -> 用户 映射
        Map<Long, AppUser> users = userMapper.selectBatchIds(items.stream().map(Danmaku::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        // 组装 DTO，昵称缺失时降级为"未知用户"/用户名
        return items.stream().map(item -> {
            AppUser sender = users.get(item.getUserId());
            return new DanmakuDto(item.getId(), item.getContent(),
                    sender == null ? "未知用户" : (sender.getName() == null ? sender.getUsername() : sender.getName()),
                    item.getCreatedAt());
        }).toList();
    }

    /**
     * 结束课堂（仅任课教师）。已结束的课堂幂等返回。
     */
    @Transactional
    public void end(Long roomId, AuthUser teacher) {
        Room room = requireRoom(roomId);
        // 权限校验：仅任课教师可结束
        if (!isTeacherOwner(room, teacher)) {
            throw BusinessException.forbidden("只有任课教师可以结束课堂");
        }
        if ("ENDED".equals(room.getStatus())) {
            return;
        }
        // 置为已结束并记录结束时间
        room.setStatus("ENDED");
        room.setEndedAt(LocalDateTime.now());
        roomMapper.updateById(room);
    }

    /**
     * 房间访问权限校验（复用 getForUser 的校验逻辑，无返回值）。
     */
    public void requireRoomAccess(Long roomId, AuthUser user) {
        getForUser(roomId, user);
    }

    /**
     * 判断用户是否为指定房间成员。
     */
    public boolean isMember(Long roomId, Long userId) {
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<RoomMember>()
                .eq(RoomMember::getRoomId, roomId).eq(RoomMember::getUserId, userId));
        return count != null && count > 0;
    }

    /**
     * 判断用户是否加入过指定课程下的任一课堂。
     */
    public boolean isMemberOfLesson(Long lessonId, Long userId) {
        // 取该课程下所有房间
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>().eq(Room::getLessonId, lessonId));
        if (rooms.isEmpty()) {
            return false;
        }
        // 在这些房间中检查用户是否为成员
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<RoomMember>()
                .in(RoomMember::getRoomId, rooms.stream().map(Room::getId).toList())
                .eq(RoomMember::getUserId, userId));
        return count != null && count > 0;
    }

    /**
     * 判断用户是否参与过指定课程的"已结束"课堂（用于学生生成课堂资料的权限）。
     */
    public boolean hasEndedRoomForLesson(Long lessonId, Long userId) {
        // 取该课程下已结束的房间
        List<Room> rooms = roomMapper.selectList(new LambdaQueryWrapper<Room>()
                .eq(Room::getLessonId, lessonId)
                .eq(Room::getStatus, "ENDED"));
        if (rooms.isEmpty()) {
            return false;
        }
        // 在这些已结束房间中检查用户是否为成员
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<RoomMember>()
                .in(RoomMember::getRoomId, rooms.stream().map(Room::getId).toList())
                .eq(RoomMember::getUserId, userId));
        return count != null && count > 0;
    }

    /**
     * 汇总指定课程所有"已结束"课堂的字幕（按时间升序），供 AI 生成课堂资料。
     */
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

    /**
     * 汇总指定课程全部课堂的字幕（按时间升序），不区分状态。
     */
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

    // 房间 + 课程 组装为 RoomDto（含 PDF 访问 URL、页数等）
    private RoomDto toDto(Room room, Lesson lesson) {
        return new RoomDto(room.getId(), room.getLessonId(), lesson.getTitle(), room.getRoomCode(),
                room.getCurrentPage(), room.getStatus(), storageService.url(lesson.getPdfPath()),
                lesson.getPageCount(), room.getCreatedAt(), room.getEndedAt());
    }

    // 关闭某课程下所有 LIVE 房间（逐个置为 ENDED 并记录结束时间）
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

    /**
     * 生成 6 位唯一房间码。
     * 最多尝试 30 次；若仍冲突则返回服务器错误。
     *
     * @return 未被占用的房间码
     */
    private String generateCode() {
        for (int i = 0; i < 30; i++) {
            // 生成 000000~999999 的 6 位数字码
            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            // 校验唯一性
            Long count = roomMapper.selectCount(new LambdaQueryWrapper<Room>().eq(Room::getRoomCode, code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法生成唯一房间码，请重试");
    }
}
