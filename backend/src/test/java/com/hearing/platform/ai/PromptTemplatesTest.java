package com.hearing.platform.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplatesTest {

    @Test
    void truncatesLongInputAndMarksIt() {
        String result = PromptTemplates.truncate("a".repeat(20), 10);

        assertThat(result).startsWith("a".repeat(10));
        assertThat(result).contains("已截取前 10 个字符");
    }

    @Test
    void preparePromptContainsRequiredSections() {
        String prompt = PromptTemplates.prepare("测试课程", "PPT 内容");

        assertThat(prompt).contains("## 教学目标", "## 重点与难点", "## 知识点提纲");
    }
}