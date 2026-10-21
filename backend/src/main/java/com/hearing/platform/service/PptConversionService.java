package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException; // 业务异常
import com.hearing.platform.config.AppProperties;     // 应用配置（LibreOffice 路径）

import org.apache.pdfbox.Loader;                       // PDFBox 加载器
import org.apache.pdfbox.pdmodel.PDDocument;          // PDF 文档
import org.apache.poi.xslf.usermodel.XMLSlideShow;    // PPTX 演示文稿
import org.apache.poi.xslf.usermodel.XSLFNotes;       // 幻灯片备注
import org.apache.poi.xslf.usermodel.XSLFShape;       // 形状
import org.apache.poi.xslf.usermodel.XSLFSlide;       // 单页幻灯片
import org.apache.poi.xslf.usermodel.XSLFTextShape;   // 文本形状
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PPT 转换与文本提取服务。
 * - convertToPdf：调用外部 LibreOffice 将 PPTX 转为 PDF（用于课堂展示）。
 * - extractText：用 Apache POI 读取每页文字与备注（供 AI 生成资料）。
 * - countPdfPages：用 PDFBox 统计 PDF 页数（用于翻页范围）。
 */
@Service
public class PptConversionService {

    // 应用配置
    private final AppProperties properties;

    // 构造器注入配置
    public PptConversionService(AppProperties properties) {
        this.properties = properties;
    }

    /**
     * 使用 LibreOffice 无头模式将 PPTX 转为 PDF。
     * 通过独立的用户配置目录与临时日志，保证并发安全与可诊断性。
     *
     * @param pptx            源 PPTX 路径
     * @param outputDirectory PDF 输出目录
     * @return 生成的 PDF 路径
     */
    public Path convertToPdf(Path pptx, Path outputDirectory) {
        Path logFile = null;
        try {
            Files.createDirectories(outputDirectory);
            // 取不含扩展名的基础名，推断预期输出的 PDF 文件名
            String baseName = pptx.getFileName().toString();
            int dot = baseName.lastIndexOf('.');
            if (dot >= 0) {
                baseName = baseName.substring(0, dot);
            }
            Path expected = outputDirectory.resolve(baseName + ".pdf");
            // 为每个存储根准备独立的 LibreOffice 用户配置目录，避免进程争用
            Path profile = outputDirectory.getParent().resolve(".libreoffice-profile");
            Files.createDirectories(profile);
            // 临时日志收集 LibreOffice 的输出
            logFile = Files.createTempFile(outputDirectory.getParent(), "lo-", ".log");
            // 组装无头转换命令
            ProcessBuilder builder = new ProcessBuilder(
                    properties.getLibreofficePath(),
                    "-env:UserInstallation=" + profile.toUri(), // 指定用户配置目录
                    "--headless",                              // 无界面
                    "--nofirststartwizard",                    // 跳过首次向导
                    "--convert-to", "pdf",                     // 转换为 PDF
                    "--outdir", outputDirectory.toAbsolutePath().toString(), // 输出目录
                    pptx.toAbsolutePath().toString()           // 输入文件
            );
            builder.redirectErrorStream(true);          // 合并错误流到标准输出
            builder.redirectOutput(logFile.toFile());   // 输出重定向到日志
            Process process = builder.start();
            // 最多等待 120 秒
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("PPT 转换超时，请检查 LibreOffice 和演示文件");
            }
            // 读取日志，退出码非 0 或目标 PDF 缺失即判定失败
            String output = Files.exists(logFile) ? Files.readString(logFile) : "";
            if (process.exitValue() != 0 || !Files.exists(expected)) {
                throw new BusinessException("PPT 转 PDF 失败：" + abbreviate(output));
            }
            return expected;
        } catch (IOException ex) {
            // 无法启动外部进程（多为路径配置问题）
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法启动 LibreOffice，请检查路径配置：" + ex.getMessage());
        } catch (InterruptedException ex) {
            // 恢复中断标志并报错
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "PPT 转换被中断");
        } finally {
            // 清理临时日志文件（尽力而为）
            if (logFile != null) {
                try {
                    Files.deleteIfExists(logFile);
                } catch (IOException ignored) {
                    // Cleanup failure should not mask the conversion result.
                }
            }
        }
    }

    /**
     * 提取 PPTX 全部文字：逐页收集正文文本框与备注，按"第 N 页"组织并拼接。
     *
     * @param pptx PPTX 路径
     * @return 结构化纯文本
     */
    public String extractText(Path pptx) {
        try (XMLSlideShow slideShow = new XMLSlideShow(Files.newInputStream(pptx))) {
            List<String> slideTexts = new ArrayList<>();
            List<XSLFSlide> slides = slideShow.getSlides();
            for (int index = 0; index < slides.size(); index++) {
                XSLFSlide slide = slides.get(index);
                List<String> pieces = new ArrayList<>();
                // 收集本页正文文本形状
                collectText(slide, pieces);
                // 收集本页备注
                XSLFNotes notes = slide.getNotes();
                if (notes != null) {
                    collectText(notes, pieces);
                }
                String joined = String.join("\n", pieces).trim();
                // 非空页才加入，并标注页码
                if (!joined.isBlank()) {
                    slideTexts.add("第 " + (index + 1) + " 页\n" + joined);
                }
            }
            // 页与页之间空行分隔
            return String.join("\n\n", slideTexts);
        } catch (IOException ex) {
            throw new BusinessException("读取 PPTX 文字失败：" + ex.getMessage());
        }
    }

    // 遍历形状集合，收集其中文本形状的非空文字
    private void collectText(Iterable<XSLFShape> shapes, List<String> pieces) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    pieces.add(text.trim());
                }
            }
        }
    }

    /**
     * 统计 PDF 页数。
     */
    public int countPdfPages(Path pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            return document.getNumberOfPages();
        } catch (IOException ex) {
            throw new BusinessException("读取 PDF 页数失败：" + ex.getMessage());
        }
    }

    // 压缩多行输出为单行并截断到 300 字符，便于放入错误消息
    private String abbreviate(String value) {
        if (value == null) {
            return "未知错误";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) + "..." : normalized;
    }
}
