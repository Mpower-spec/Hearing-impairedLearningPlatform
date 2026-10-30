package com.hearing.platform.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CourseQaServiceTest {

    @Test
    void extractsChineseBigramsAndIgnoresCommonQuestionWords() {
        var terms = CourseQaService.queryTerms("光合作用需要哪些条件？");

        assertThat(terms).contains("光合", "合作", "作用", "条件");
        assertThat(terms).doesNotContain("哪些", "需要");
    }

    @Test
    void convertsMarkdownAnswerToPlainText() {
        String markdown = """
                # 学习重点

                **光合作用**需要光照、水和二氧化碳。

                - 光照
                - 水
                - 二氧化碳

                ```text
                参考 [S1]
                ```
                [查看资料](https://example.com)
                """;

        String plain = CourseQaService.plainTextAnswer(markdown);

        assertThat(plain)
                .contains("学习重点", "光合作用需要光照", "• 光照", "参考 [S1]", "查看资料")
                .doesNotContain("#", "**", "```", "](");
    }

    @Test
    void scoresRelevantCourseContentHigher() {
        var terms = CourseQaService.queryTerms("光合作用需要什么条件");

        int relevant = CourseQaService.score("植物进行光合作用需要阳光、水和二氧化碳。", "光合作用需要什么条件", terms);
        int unrelated = CourseQaService.score("今天学习课堂纪律和讨论规则。", "光合作用需要什么条件", terms);

        assertThat(relevant).isGreaterThan(unrelated);
        assertThat(relevant).isGreaterThan(0);
    }
}
