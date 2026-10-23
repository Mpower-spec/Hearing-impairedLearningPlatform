package com.hearing.platform.service;

import com.hearing.platform.dao.CaptionMapper;
import com.hearing.platform.dao.DanmakuMapper;
import com.hearing.platform.dao.LessonMapper;
import com.hearing.platform.dao.RoomMapper;
import com.hearing.platform.dao.RoomMemberMapper;
import com.hearing.platform.dao.UserMapper;
import com.hearing.platform.dto.RoomDto;
import com.hearing.platform.entity.Lesson;
import com.hearing.platform.entity.Room;
import com.hearing.platform.entity.RoomMember;
import com.hearing.platform.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceStudentHistoryTest {

    @Mock private RoomMapper roomMapper;
    @Mock private RoomMemberMapper memberMapper;
    @Mock private CaptionMapper captionMapper;
    @Mock private DanmakuMapper danmakuMapper;
    @Mock private LessonMapper lessonMapper;
    @Mock private LessonService lessonService;
    @Mock private UserMapper userMapper;
    @Mock private FileStorageService storageService;

    private RoomService roomService;
    private final AuthUser student = new AuthUser(2L, "student", "演示学生", "STUDENT");

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomMapper, memberMapper, captionMapper, danmakuMapper,
                lessonMapper, lessonService, userMapper, storageService);
    }

    @Test
    void listsLiveAndEndedRoomsJoinedByCurrentStudent() {
        RoomMember liveMembership = membership(32L, 2L);
        RoomMember endedMembership = membership(31L, 2L);
        Room liveRoom = room(32L, 12L, "LIVE", "131728");
        Room endedRoom = room(31L, 12L, "ENDED", "934872");
        Lesson lesson = lesson();

        when(memberMapper.selectList(any())).thenReturn(List.of(liveMembership, endedMembership));
        when(roomMapper.selectList(any())).thenReturn(List.of(liveRoom, endedRoom));
        when(lessonMapper.selectBatchIds(any())).thenReturn(List.of(lesson));
        when(storageService.url("lesson.pdf")).thenReturn("/files/lesson.pdf");

        List<RoomDto> result = roomService.listForStudent(student);

        assertThat(result).extracting(RoomDto::id).containsExactly(32L, 31L);
        assertThat(result).extracting(RoomDto::status).containsExactly("LIVE", "ENDED");
        assertThat(result).extracting(RoomDto::lessonTitle).containsOnly("演示课程");
    }

    @Test
    void restJoinPersistsMembershipBeforeReturningRoom() {
        Room liveRoom = room(32L, 12L, "LIVE", "131728");
        Lesson lesson = lesson();
        when(roomMapper.selectOne(any())).thenReturn(liveRoom);
        when(lessonMapper.selectById(12L)).thenReturn(lesson);
        when(storageService.url("lesson.pdf")).thenReturn("/files/lesson.pdf");

        RoomDto result = roomService.joinByCodeForStudent("131728", student);

        verify(memberMapper).insertIgnore(eq(32L), eq(2L), any(LocalDateTime.class));
        assertThat(result.id()).isEqualTo(32L);
        assertThat(result.roomCode()).isEqualTo("131728");
    }

    private RoomMember membership(Long roomId, Long userId) {
        RoomMember member = new RoomMember();
        member.setRoomId(roomId);
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        return member;
    }

    private Room room(Long id, Long lessonId, String status, String roomCode) {
        Room room = new Room();
        room.setId(id);
        room.setLessonId(lessonId);
        room.setStatus(status);
        room.setRoomCode(roomCode);
        room.setCurrentPage(1);
        room.setCreatedAt(LocalDateTime.now());
        return room;
    }

    private Lesson lesson() {
        Lesson lesson = new Lesson();
        lesson.setId(12L);
        lesson.setTitle("演示课程");
        lesson.setTeacherId(1L);
        lesson.setPdfPath("lesson.pdf");
        lesson.setPageCount(8);
        return lesson;
    }
}