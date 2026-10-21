package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;
import com.hearing.platform.dto.CaptionDto;
import com.hearing.platform.dto.CreateRoomRequest;
import com.hearing.platform.dto.DanmakuDto;
import com.hearing.platform.dto.RoomDto;
import com.hearing.platform.security.AuthContext;
import com.hearing.platform.security.RequireRole;
import com.hearing.platform.security.Roles;
import com.hearing.platform.service.RoomService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @RequireRole(Roles.TEACHER)
    public ApiResponse<RoomDto> create(@Valid @RequestBody CreateRoomRequest request) {
        return ApiResponse.ok("课堂已创建", roomService.create(AuthContext.get(), request.lessonId()));
    }

    @GetMapping("/mine")
    @RequireRole(Roles.TEACHER)
    public ApiResponse<List<RoomDto>> mine() {
        return ApiResponse.ok(roomService.listForTeacher(AuthContext.get()));
    }

    @GetMapping("/by-code/{roomCode}")
    public ApiResponse<RoomDto> byCode(@PathVariable String roomCode) {
        return ApiResponse.ok(roomService.previewByCode(roomCode, AuthContext.get()));
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(roomService.getForUser(id, AuthContext.get()));
    }

    @PostMapping("/{id}/end")
    @RequireRole(Roles.TEACHER)
    public ApiResponse<Void> end(@PathVariable Long id) {
        roomService.end(id, AuthContext.get());
        return ApiResponse.ok("课堂已结束", null);
    }

    @GetMapping("/{id}/captions")
    public ApiResponse<List<CaptionDto>> captions(@PathVariable Long id) {
        return ApiResponse.ok(roomService.captions(id, AuthContext.get()));
    }

    @GetMapping("/{id}/danmaku")
    public ApiResponse<List<DanmakuDto>> danmaku(@PathVariable Long id) {
        return ApiResponse.ok(roomService.danmaku(id, AuthContext.get()));
    }
}