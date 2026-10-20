package com.hearing.platform.lesson;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("lesson")
public class Lesson {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("teacher_id")
    private Long teacherId;
    private String title;
    @TableField("pptx_path")
    private String pptxPath;
    @TableField("pdf_path")
    private String pdfPath;
    @TableField("ppt_text")
    private String pptText;
    @TableField("page_count")
    private Integer pageCount;
    private String status;
    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPptxPath() { return pptxPath; }
    public void setPptxPath(String pptxPath) { this.pptxPath = pptxPath; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public String getPptText() { return pptText; }
    public void setPptText(String pptText) { this.pptText = pptText; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}