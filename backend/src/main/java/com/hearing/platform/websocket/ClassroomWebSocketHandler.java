package com.hearing.platform.websocket;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.dto.CaptionDto;           // 字幕展示对象
import com.hearing.platform.dto.DanmakuDto;           // 弹幕展示对象
import com.hearing.platform.dto.RoomDto;              // 课堂房间展示对象
import com.hearing.platform.entity.Room;              // 课堂房间实体
import com.hearing.platform.security.AuthUser;        // 已认证用户
import com.hearing.platform.security.JwtService;      // JWT 解析服务
import com.hearing.platform.service.RoomService;      // 课堂房间业务服务

import com.fasterxml.jackson.databind.JsonNode;       // JSON 节点
import com.fasterxml.jackson.databind.ObjectMapper;   // JSON 序列化/反序列化
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder; // 解析 URI 查询参数

import java.util.LinkedHashMap;                       // 保持插入顺序的 Map
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;        // 并发安全的参与者登记表

/**
 * 课堂实时交互处理器。
 * 基于文本 WebSocket，处理加入课堂、翻页、字幕、弹幕、结束课堂等消息，
 * 并按房间维度向所有在线参与者广播。连接建立时用 URL 中的 token 完成鉴权。
 */
@Component
public class ClassroomWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ClassroomWebSocketHandler.class);
    // 会话属性键：存放鉴权后的用户
    private static final String AUTH_ATTRIBUTE = "authUser";

    // JWT 服务
    private final JwtService jwtService;
    // 课堂房间服务
    private final RoomService roomService;
    // JSON 工具
    private final ObjectMapper objectMapper;
    // 在线参与者表：sessionId -> Participant
    private final Map<String, Participant> participants = new ConcurrentHashMap<>();

    // 构造器注入依赖
    public ClassroomWebSocketHandler(JwtService jwtService, RoomService roomService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.roomService = roomService;
        this.objectMapper = objectMapper;
    }

    /**
     * 连接建立后：从 URL 查询参数取 token 并鉴权。
     * 缺失或无效令牌则直接关闭连接。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 取出 ?token= 参数
        String token = UriComponentsBuilder.fromUri(session.getUri()).build()
                .getQueryParams().getFirst("token");
        if (token == null || token.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing token"));
            return;
        }
        try {
            // 解析令牌，成功则把用户挂到会话属性上
            AuthUser user = jwtService.parseToken(token);
            session.getAttributes().put(AUTH_ATTRIBUTE, user);
        } catch (BusinessException ex) {
            // 令牌无效：拒绝连接
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
        }
    }

    /**
     * 文本消息总入口：解析 JSON，按 type 字段分发到对应处理方法。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 未鉴权的连接直接关闭
        AuthUser user = (AuthUser) session.getAttributes().get(AUTH_ATTRIBUTE);
        if (user == null) {
            closeQuietly(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        try {
            // 解析消息体，读取消息类型
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText("");
            // join 是唯一无需先在参与者表中的消息（用于建立参与关系）
            if ("join".equals(type)) {
                handleJoin(session, user, payload);
                return;
            }
            // 其他消息必须先加入课堂
            Participant participant = participants.get(session.getId());
            if (participant == null) {
                sendError(session, "请先加入课堂");
                return;
            }
            // 按类型分发处理
            switch (type) {
                case "slide" -> handleSlide(participant, payload);      // 翻页
                case "caption" -> handleCaption(participant, payload);  // 字幕
                case "danmaku" -> handleDanmaku(participant, payload);  // 弹幕
                case "end" -> handleEnd(participant);                   // 结束课堂
                case "ping" -> send(session, Map.of("type", "pong"));   // 心跳
                default -> sendError(session, "未知消息类型");
            }
        } catch (BusinessException ex) {
            // 业务异常：把可读消息回传给客户端
            sendError(session, ex.getMessage());
        } catch (Exception ex) {
            // 其他异常：记录日志并回传通用错误
            log.warn("WebSocket message failed", ex);
            sendError(session, "消息处理失败");
        }
    }

    /**
     * 处理加入课堂：按房间码加入、登记参与者、回传当前课堂完整状态。
     */
    private void handleJoin(WebSocketSession session, AuthUser user, JsonNode payload) throws Exception {
        String roomCode = payload.path("roomCode").asText("").trim();
        if (roomCode.isBlank()) {
            sendError(session, "请输入房间码");
            return;
        }
        // 服务层校验房间码并完成加入（学生会自动登记成员）
        Room room = roomService.joinByCode(roomCode, user);
        // 将会话登记为该房间的参与者
        participants.put(session.getId(), new Participant(session, user, room.getId(), roomCode));
        // 组装课堂状态并回传给刚加入的客户端
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

    /**
     * 处理翻页（仅教师）：更新当前页并向全房间广播。
     */
    private void handleSlide(Participant participant, JsonNode payload) {
        requireTeacher(participant);
        int page = payload.path("page").asInt(0);
        roomService.updatePage(participant.roomId(), page);
        broadcast(participant.roomId(), Map.of("type", "slide", "page", page));
    }

    /**
     * 处理字幕（仅教师）：持久化后向全房间广播。
     */
    private void handleCaption(Participant participant, JsonNode payload) {
        requireTeacher(participant);
        CaptionDto caption = roomService.saveCaption(participant.roomId(), payload.path("text").asText(""));
        // 组装字幕广播消息
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "caption");
        message.put("id", caption.id());
        message.put("text", caption.content());
        message.put("createdAt", caption.createdAt().toString());
        broadcast(participant.roomId(), message);
    }

    /**
     * 处理弹幕（教师/学生均可）：持久化后向全房间广播（含发送者昵称）。
     */
    private void handleDanmaku(Participant participant, JsonNode payload) {
        DanmakuDto danmaku = roomService.saveDanmaku(participant.roomId(), participant.user(),
                payload.path("content").asText(""));
        // 组装弹幕广播消息
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "danmaku");
        message.put("id", danmaku.id());
        message.put("content", danmaku.content());
        message.put("userName", danmaku.userName());
        message.put("createdAt", danmaku.createdAt().toString());
        broadcast(participant.roomId(), message);
    }

    /**
     * 处理结束课堂（仅教师）：结束房间并向全房间广播结束事件。
     */
    private void handleEnd(Participant participant) {
        requireTeacher(participant);
        roomService.end(participant.roomId(), participant.user());
        broadcast(participant.roomId(), Map.of("type", "end"));
    }

    // 教师权限校验：非教师抛出禁止异常
    private void requireTeacher(Participant participant) {
        if (!participant.user().isTeacher()) {
            throw BusinessException.forbidden("只有教师可以执行此操作");
        }
    }

    /**
     * 向指定房间的所有在线参与者广播消息。
     */
    private void broadcast(Long roomId, Map<String, Object> message) {
        participants.values().stream()
                .filter(item -> item.roomId().equals(roomId)) // 仅同房间
                .forEach(item -> send(item.session(), message));
    }

    // 向客户端回传错误消息
    private void sendError(WebSocketSession session, String message) {
        send(session, Map.of("type", "error", "message", message == null ? "未知错误" : message));
    }

    /**
     * 安全发送：会话开启时序列化并发送；对同一会话加锁避免并发写冲突。
     */
    private void send(WebSocketSession session, Map<String, Object> message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) { // WebSocketSession 非线程安全，发送需串行化
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception ex) {
            log.debug("WebSocket send failed", ex);
        }
    }

    /**
     * 连接关闭后：从参与者表移除，避免内存泄漏。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        participants.remove(session.getId());
    }

    /**
     * 传输异常：清理参与者并静默关闭会话。
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        participants.remove(session.getId());
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    // 静默关闭会话：忽略关闭过程中的异常
    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (Exception ignored) {
            // Nothing useful can be done after a transport failure.
        }
    }

    // 参与者信息：会话、用户、所加入的房间 ID 与房间码
    private record Participant(WebSocketSession session, AuthUser user, Long roomId, String roomCode) {
    }
}
