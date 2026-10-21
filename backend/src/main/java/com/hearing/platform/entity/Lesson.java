package com.hearing.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType; // 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 字段映射注解
import com.baomidou.mybatisplus.annotation.TableId; // 主键映射注解
import com.baomidou.mybatisplus.annotation.TableName; // 表名映射注解

import java.time.LocalDateTime; // 时间戳类型

/**
 * 课件实体。
 * 对应数据库表 lesson。
 * 记录教师上传的 PPT 及其转换/抽取结果。
 */
@TableName("lesson") // 映射到 lesson 表
public class Lesson {
    @TableId(type = IdType.AUTO) // 主键，数据库自增
    private Long id; // 课件 ID
    @TableField("teacher_id") // 对应列 teacher_id
    private Long teacherId; // 归属教师 ID
    private String title; // 课件标题
    @TableField("pptx_path") // 对应列 pptx_path
    private String pptxPath; // 原始 PPTX 存储路径
    @TableField("pdf_path") // 对应列 pdf_path
    private String pdfPath; // 转换后的 PDF 存储路径
    @TableField("ppt_text") // 对应列 ppt_text
    private String pptText; // 从 PPT 抽取的文字内容
    @TableField("page_count") // 对应列 page_count
    private Integer pageCount; // 页数
    private String status; // 处理状态
    @TableField("created_at") // 对应列 created_at
    private LocalDateTime createdAt; // 创建时间

    public Long getId() { return id; } // 读取课件 ID
    public void setId(Long id) { this.id = id; } // 写入课件 ID
    public Long getTeacherId() { return teacherId; } // 读取教师 ID
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; } // 写入教师 ID
    public String getTitle() { return title; } // 读取标题
    public void setTitle(String title) { this.title = title; } // 写入标题
    public String getPptxPath() { return pptxPath; } // 读取 PPTX 路径
    public void setPptxPath(String pptxPath) { this.pptxPath = pptxPath; } // 写入 PPTX 路径
    public String getPdfPath() { return pdfPath; } // 读取 PDF 路径
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; } // 写入 PDF 路径
    public String getPptText() { return pptText; } // 读取抽取文字
    public void setPptText(String pptText) { this.pptText = pptText; } // 写入抽取文字
    public Integer getPageCount() { return pageCount; } // 读取页数
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; } // 写入页数
    public String getStatus() { return status; } // 读取状态
    public void setStatus(String status) { this.status = status; } // 写入状态
    public LocalDateTime getCreatedAt() { return createdAt; } // 读取创建时间
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; } // 写入创建时间
}
