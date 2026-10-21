package com.hearing.platform.dao;

import com.hearing.platform.entity.RoomMember; // 房间成员实体

import com.baomidou.mybatisplus.core.mapper.BaseMapper; // MyBatis-Plus 通用 Mapper 基类
import org.apache.ibatis.annotations.Insert; // 插入 SQL 注解
import org.apache.ibatis.annotations.Param; // 参数命名注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 房间成员数据访问接口。
 * 继承 BaseMapper，并补充一个幂等插入方法。
 */
public interface RoomMemberMapper extends BaseMapper<RoomMember> {

    /**
     * 忽略重复插入。
     * 借助 INSERT IGNORE，同一房间同一用户重复加入不报错。
     * @param roomId 房间 ID
     * @param userId 用户 ID
     * @param joinedAt 加入时间
     * @return 实际插入的行数（重复时为 0）
     */
    @Insert("INSERT IGNORE INTO room_member(room_id, user_id, joined_at) VALUES(#{roomId}, #{userId}, #{joinedAt})")
    int insertIgnore(@Param("roomId") Long roomId, @Param("userId") Long userId,
                     @Param("joinedAt") LocalDateTime joinedAt);
}
