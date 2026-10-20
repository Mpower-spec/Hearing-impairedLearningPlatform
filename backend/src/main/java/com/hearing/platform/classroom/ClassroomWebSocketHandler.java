package com.hearing.platform.classroom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hearing.platform.auth.AuthUser;
import com.hearing.platform.auth.JwtService;
import com.hearing.platform.common.BusinessException;
import com.hearing.platform.room.CaptionDto;
import com.hearing.platform.room.DanmakuDto;
import com.hearing.platform.room.Room;
import com.hearing.platform.room.RoomDto;
import com.hearing.platform.room.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClassroomWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ClassroomWebSocketHandler.class);
    private static final String AUTH_ATTRIBUTE = "authUser";

    private final JwtService jwtService;
    private final RoomService roomService;
    private final ObjectMapper objectMapper;
    private final Map<String, Participant> participants = new ConcurrentHashMap<>();

    public ClassroomWebSocketHandler(JwtService jwtService, RoomService roomService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.roomService = roomService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = UriComponentsBuilder.fromUri(session.getUri()).build()
                .getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing token"));
            return;
        }
        try {
            AuthUser user = jwtService.parseToken(token);
            session.getAttributes().put(AUTH_ATTRIBUTE, user);
        } catch (BusinessException ex) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        AuthUser user = (AuthUser) session.getAttributes().get(AUTH_ATTRIBUTE);
        if (user == null) {
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText("");
            if ("join".equals(type)) {
                handleJoin(session, user, payload);
                return;
            }
            Participant participant = participants.get(session.getId());
            if (participant == null) {
                sendError(session, "请先加入课堂");
                return;
            }
            switch (type) {
                case "slide" -> handleSlide(participant, payload);
                case "caption" -> handleCaption(participant, payload);
                case "danmaku" -> handleDanmaku(participant, payload);
                case "end" -> handleEnd(participant);
                case "ping" -> send(session, Map.of("type", "pong"));
                default -> sendError(session, "未知消息类型");
            }
        } catch (BusinessException ex) {
            sendError(session, ex.getMessage());
        } catch (Exception ex) {
            log.warn("WebSocket message failed", ex);
            sendError(session, "消息处理失败");
        }
    }

    private void handleJoin(WebSocketSession session, AuthUser user, JsonNode payload) throws Exception {
        String roomCode = payload.path("roomCode").asText("").trim();
        if (roomCode.isBlank()) {
            sendError(session, "请输入房间码");
            return;
        }
        Room room = roomService.joinByCode(roomCode, user);
        participants.put(session.getId(), new Participant(session, user, room.getId(), roomCode));
        RoomDto dto = roomService.getForUser(room.getId(), user);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("type", "state");
        state.put("roomId", dto.id());
        state.put("lessonId", dto.lessonId());
        state.put("lessonTitle", dto.lessonTitle());
        state.put("roomCode", dto.roomCode());
        state.put("page", dto.currentPage());
        state.put("status", dto.status());
        state.put("pdfUrl", dto.pdfUrl());
        state.put("pageCount", dto.pageCount());
        send(session, state);
    }

    private void handleSlide(Participant participant, JsonNode payload) {
        requireTeacher(participant);
        int page = payload.path("page").asInt(0);
        roomService.updatePage(participant.roomId(), page);
        broadcast(participant.roomId(), Map.of("type", "slide", "page", page));
    }

    private void handleCaption(Participant participant, JsonNode payload) {
        requireTeacher(participant);
        CaptionDto caption = roomService.saveCaption(participant.roomId(), payload.path("text").asText(""));
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "caption");
        message.put("id", caption.id());
        message.put("text", caption.content());
        message.put("createdAt", caption.createdAt().toString());
        broadcast(participant.roomId(), message);
    }

    private void handleDanmaku(Participant participant, JsonNode payload) {
        DanmakuDto danmaku = roomService.saveDanmaku(participant.roomId(), participant.user(),
                payload.path("content").asText(""));
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "danmaku");
        message.put("id", danmaku.id());
        message.put("content", danmaku.content());
        message.put("userName", danmaku.userName());
        message.put("createdAt", danmaku.createdAt().toString());
        broadcast(participant.roomId(), message);
    }

    private void handleEnd(Participant participant) {
        requireTeacher(participant);
        roomService.end(participant.roomId(), participant.user());
        broadcast(participant.roomId(), Map.of("type", "end"));
    }

    private void requireTeacher(Participant participant) {
        if (!participant.user().isTeacher()) {
            throw BusinessException.forbidden("只有教师可以执行此操作");
        }
    }

    private void broadcast(Long roomId, Map<String, Object> message) {
        participants.values().stream()
                .filter(item -> item.roomId().equals(roomId))
                .forEach(item -> send(item.session(), message));
    }

    private void sendError(WebSocketSession session, String message) {
        send(session, Map.of("type", "error", "message", message == null ? "未知错误" : message));
    }

    private void send(WebSocketSession session, Map<String, Object> message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception ex) {
            log.debug("WebSocket send failed", ex);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        participants.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        participants.remove(session.getId());
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (Exception ignored) {
            // Nothing useful can be done after a transport failure.
        }
    }

    private record Participant(WebSocketSession session, AuthUser user, Long roomId, String roomCode) {
    }
}