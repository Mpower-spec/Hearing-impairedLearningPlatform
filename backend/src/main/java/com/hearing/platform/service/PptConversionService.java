package com.hearing.platform.service;

import com.hearing.platform.common.BusinessException;
import com.hearing.platform.config.AppProperties;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PptConversionService {

    private final AppProperties properties;

    public PptConversionService(AppProperties properties) {
        this.properties = properties;
    }

    public Path convertToPdf(Path pptx, Path outputDirectory) {
        Path logFile = null;
        try {
            Files.createDirectories(outputDirectory);
            String baseName = pptx.getFileName().toString();
            int dot = baseName.lastIndexOf('.');
            if (dot >= 0) {
                baseName = baseName.substring(0, dot);
            }
            Path expected = outputDirectory.resolve(baseName + ".pdf");
            Path profile = outputDirectory.getParent().resolve(".libreoffice-profile");
            Files.createDirectories(profile);
            logFile = Files.createTempFile(outputDirectory.getParent(), "lo-", ".log");
            ProcessBuilder builder = new ProcessBuilder(
                    properties.getLibreofficePath(),
                    "-env:UserInstallation=" + profile.toUri(),
                    "--headless",
                    "--nofirststartwizard",
                    "--convert-to", "pdf",
                    "--outdir", outputDirectory.toAbsolutePath().toString(),
                    pptx.toAbsolutePath().toString()
            );
            builder.redirectErrorStream(true);
            builder.redirectOutput(logFile.toFile());
            Process process = builder.start();
            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("PPT 转换超时，请检查 LibreOffice 和演示文件");
            }
            String output = Files.exists(logFile) ? Files.readString(logFile) : "";
            if (process.exitValue() != 0 || !Files.exists(expected)) {
                throw new BusinessException("PPT 转 PDF 失败：" + abbreviate(output));
            }
            return expected;
        } catch (IOException ex) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "无法启动 LibreOffice，请检查路径配置：" + ex.getMessage());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "PPT 转换被中断");
        } finally {
            if (logFile != null) {
                try {
                    Files.deleteIfExists(logFile);
                } catch (IOException ignored) {
                    // Cleanup failure should not mask the conversion result.
                }
            }
        }
    }

    public String extractText(Path pptx) {
        try (XMLSlideShow slideShow = new XMLSlideShow(Files.newInputStream(pptx))) {
            List<String> slideTexts = new ArrayList<>();
            List<XSLFSlide> slides = slideShow.getSlides();
            for (int index = 0; index < slides.size(); index++) {
                XSLFSlide slide = slides.get(index);
                List<String> pieces = new ArrayList<>();
                collectText(slide, pieces);
                XSLFNotes notes = slide.getNotes();
                if (notes != null) {
                    collectText(notes, pieces);
                }
                String joined = String.join("\n", pieces).trim();
                if (!joined.isBlank()) {
                    slideTexts.add("第 " + (index + 1) + " 页\n" + joined);
                }
            }
            return String.join("\n\n", slideTexts);
        } catch (IOException ex) {
            throw new BusinessException("读取 PPTX 文字失败：" + ex.getMessage());
        }
    }

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

    public int countPdfPages(Path pdf) {
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            return document.getNumberOfPages();
        } catch (IOException ex) {
            throw new BusinessException("读取 PDF 页数失败：" + ex.getMessage());
        }
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "未知错误";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) + "..." : normalized;
    }
}