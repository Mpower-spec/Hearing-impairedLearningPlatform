package com.hearing.platform.dao;

import com.hearing.platform.entity.Danmaku; // 弹幕实体

import com.baomidou.mybatisplus.core.mapper.BaseMapper; // MyBatis-Plus 通用 Mapper 基类

/**
 * 弹幕数据访问接口。
 * 继承 BaseMapper 后自带增删改查能力。
 */
public interface DanmakuMapper extends BaseMapper<Danmaku> {
}
