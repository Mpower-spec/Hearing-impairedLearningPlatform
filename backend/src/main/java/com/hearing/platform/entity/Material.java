package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 字段映射注解
import com.baomidou.mybatisplus.annotation.TableId; // 主键映射注解
import com.baomidou.mybatisplus.annotation.TableName; // 表名映射注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 学习资料实体。
 * 对应数据库表 material。
 * 保存由课件 AI 生成的讲解、摘要等衍生内容。
 */
@TableName("material") // 映射到 material 表
public class Material {
    @TableId(type = IdType.AUTO) // 主键，数据库自增
    private Long id; // 资料 ID
    @TableField("lesson_id") // 对应列 lesson_id
    private Long lessonId; // 关联课件 ID
    private String type; // 资料类型（如讲解/摘要）
    private String content; // 资料正文内容
    private String source; // 内容来源（real/mock）
    @TableField("created_at") // 对应列 created_at
    private LocalDateTime createdAt; // 创建时间
    @TableField("updated_at") // 对应列 updated_at
    private LocalDateTime updatedAt; // 更新时间

    public Long getId() { return id; } // 读取资料 ID
    public void setId(Long id) { this.id = id; } // 写入资料 ID
    public Long getLessonId() { return lessonId; } // 读取课件 ID
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; } // 写入课件 ID
    public String getType() { return type; } // 读取类型
    public void setType(String type) { this.type = type; } // 写入类型
    public String getContent() { return content; } // 读取正文
    public void setContent(String content) { this.content = content; } // 写入正文
    public String getSource() { return source; } // 读取来源
    public void setSource(String source) { this.source = source; } // 写入来源
    public LocalDateTime getCreatedAt() { return createdAt; } // 读取创建时间
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // 写入创建时间
    public LocalDateTime getUpdatedAt() { return updatedAt; } // 读取更新时间
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; } // 写入更新时间
}
