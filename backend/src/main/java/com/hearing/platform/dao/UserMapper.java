package com.hearing.platform.dao;

import com.hearing.platform.entity.AppUser; // 用户实体

import com.baomidou.mybatisplus.core.mapper.BaseMapper; // MyBatis-Plus 通用 Mapper 基类

/**
 * 用户数据访问接口。
 * 继承 BaseMapper 后自带增删改查能力。
 */
public interface UserMapper extends BaseMapper<AppUser> {
}
