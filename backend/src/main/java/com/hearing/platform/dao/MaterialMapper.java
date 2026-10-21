package com.hearing.platform.dao;

import com.hearing.platform.entity.Material; // 学习资料实体

import com.baomidou.mybatisplus.core.mapper.BaseMapper; // MyBatis-Plus 通用 Mapper 基类

/**
 * 学习资料数据访问接口。
 * 继承 BaseMapper 后自带增删改查能力。
 */
public interface MaterialMapper extends BaseMapper<Material> {
}
