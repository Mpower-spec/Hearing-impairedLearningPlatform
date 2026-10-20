package com.hearing.platform.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VttServiceTest {

    private final VttService service = new VttService();

    @Test
    void createsOneCuePerSentenceAndCoversVideoDuration() {
        String vtt = service.createEstimatedVtt("第一句。第二句！第三句？", 9.0);

        assertThat(vtt).startsWith("WEBVTT\n\n");
        assertThat(vtt.split("-->", -1)).hasSize(4);
        assertThat(vtt).contains("00:00:00.000 -->");
        assertThat(vtt).contains("00:00:09.000");
        assertThat(vtt).contains("第一句。").contains("第二句！").contains("第三句？");
    }

    @Test
    void removesSymbolOnlyCuesAndKeepsNonOverlappingTimeline() {
        String vtt = service.createEstimatedVtt("第一句。😊", 10.0);

        assertThat(vtt).doesNotContain("😊");
        assertThat(vtt.split("-->", -1)).hasSize(2);
    }

    @Test
    void handlesEmptyTranscriptWithReadablePlaceholder() {
        String vtt = service.createEstimatedVtt("", 2.0);

        assertThat(vtt).contains("未识别到有效字幕内容");
        assertThat(vtt).contains("-->");
    }
}