package com.hearing.platform.dao;

import com.hearing.platform.entity.RoomMember;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface RoomMemberMapper extends BaseMapper<RoomMember> {

    @Insert("INSERT IGNORE INTO room_member(room_id, user_id, joined_at) VALUES(#{roomId}, #{userId}, #{joinedAt})")
    int insertIgnore(@Param("roomId") Long roomId, @Param("userId") Long userId,
                     @Param("joinedAt") LocalDateTime joinedAt);
}