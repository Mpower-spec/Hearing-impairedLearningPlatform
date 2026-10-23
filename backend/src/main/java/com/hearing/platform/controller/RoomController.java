package com.hearing.platform.controller;

import com.hearing.platform.common.ApiResponse;   // 统一响应封装体
import com.hearing.platform.dto.CaptionDto;        // 字幕展示对象
import com.hearing.platform.dto.CreateRoomRequest; // 创建课堂请求参数
import com.hearing.platform.dto.DanmakuDto;        // 弹幕展示对象
import com.hearing.platform.dto.JoinRoomRequest;    // 学生加入课堂请求
import com.hearing.platform.dto.RoomDto;           // 课堂房间展示对象
import com.hearing.platform.security.AuthContext;  // 当前登录用户上下文
import com.hearing.platform.security.RequireRole;  // 角色访问控制注解
import com.hearing.platform.security.Roles;        // 角色常量定义
import com.hearing.platform.service.RoomService;   // 课堂房间业务服务

import jakarta.validation.Valid;                   // 入参校验注解
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课堂房间接口控制器。
 * 提供课堂的创建、查询、结束以及字幕/弹幕历史读取等接口，统一前缀 /api/rooms。
 * 实时互动（翻页、发送字幕/弹幕）走 WebSocket，此处主要是 REST 侧的管理与回放。
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    // 课堂房间业务服务，构造器注入
    private final RoomService roomService;

    // 构造器注入 RoomService
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * 创建课堂房间。
     * 权限：仅教师；会为该课程生成唯一房间码，并关闭同课程已有的直播房间。
     *
     * @param request 创建请求体（含 lessonId）
     * @return 新建课堂信息
     */
    @PostMapping
    @RequireRole(Roles.TEACHER)
    public ApiResponse<RoomDto> create(@Valid @RequestBody CreateRoomRequest request) {
        // 服务层校验课程归属后创建房间
        return ApiResponse.ok("课堂已创建", roomService.create(AuthContext.get(), request.lessonId()));
    }

    /**
     * 查询当前教师名下的全部课堂。
     * 权限：仅教师。
     *
     * @return 课堂列表
     */
    @GetMapping("/mine")
    @RequireRole(Roles.TEACHER)
    public ApiResponse<List<RoomDto>> mine() {
        return ApiResponse.ok(roomService.listForTeacher(AuthContext.get()));
    }

    /**
     * 按房间码预览课堂（加入前的信息确认）。
     *
     * @param roomCode 6 位房间码
     * @return 课堂概要信息
     */
    /**
     * 查询当前学生加入过的课堂记录，包含进行中和已结束课堂。
     */
    @GetMapping("/joined")
    @RequireRole(Roles.STUDENT)
    public ApiResponse<List<RoomDto>> joined() {
        return ApiResponse.ok(roomService.listForStudent(AuthContext.get()));
    }

    /**
     * 学生通过房间码加入课堂并立即写入成员关系。
     */
    @PostMapping("/join")
    @RequireRole(Roles.STUDENT)
    public ApiResponse<RoomDto> join(@Valid @RequestBody JoinRoomRequest request) {
        return ApiResponse.ok("已加入课堂", roomService.joinByCodeForStudent(request.roomCode(), AuthContext.get()));
    }
    @GetMapping("/by-code/{roomCode}")
    public ApiResponse<RoomDto> byCode(@PathVariable String roomCode) {
        // 服务层校验房间码有效性及教师归属后返回预览
        return ApiResponse.ok(roomService.previewByCode(roomCode, AuthContext.get()));
    }

    /**
     * 查询指定课堂详情。
     *
     * @param id 课堂 ID
     * @return 课堂详情
     */
    @GetMapping("/{id}")
    public ApiResponse<RoomDto> detail(@PathVariable Long id) {
        // 服务层根据角色做访问校验（教师需归属、学生需为成员）
        return ApiResponse.ok(roomService.getForUser(id, AuthContext.get()));
    }

    /**
     * 结束指定课堂。
     * 权限：仅任课教师。
     *
     * @param id 课堂 ID
     * @return 无数据的操作结果
     */
    @PostMapping("/{id}/end")
    @RequireRole(Roles.TEACHER)
    public ApiResponse<Void> end(@PathVariable Long id) {
        // 服务层校验教师归属后将状态置为已结束
        roomService.end(id, AuthContext.get());
        return ApiResponse.ok("课堂已结束", null);
    }

    /**
     * 查询指定课堂的字幕历史。
     *
     * @param id 课堂 ID
     * @return 字幕列表（按时间升序）
     */
    @GetMapping("/{id}/captions")
    public ApiResponse<List<CaptionDto>> captions(@PathVariable Long id) {
        // 服务层先做访问权限校验再返回字幕
        return ApiResponse.ok(roomService.captions(id, AuthContext.get()));
    }

    /**
     * 查询指定课堂的弹幕历史。
     *
     * @param id 课堂 ID
     * @return 弹幕列表（含发送者昵称，按时间升序）
     */
    @GetMapping("/{id}/danmaku")
    public ApiResponse<List<DanmakuDto>> danmaku(@PathVariable Long id) {
        // 服务层先做访问权限校验再返回弹幕
        return ApiResponse.ok(roomService.danmaku(id, AuthContext.get()));
    }
}
