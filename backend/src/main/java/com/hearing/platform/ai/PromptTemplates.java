package com.hearing.platform.ai;

/**
 * 提示词模板。
 * 集中定义面向大模型的系统提示与各任务（备课/课堂资料/视频笔记）的用户提示，
 * 统一约束输出为适合听障学生阅读的 Markdown，并对输入长度做截断。
 */
public final class PromptTemplates {

    // 私有构造，禁止实例化（纯静态模板类）
    private PromptTemplates() {
    }

    // 系统提示词：设定助手角色、语言风格与"不编造事实"等约束
    public static String system() {
        return "你是一名严谨、耐心的特殊教育学习支持助手。请使用清晰、简洁、适合听障学生阅读的中文，"
                + "输出完整 Markdown，不要使用代码块包裹整篇结果，不要编造材料中不存在的事实。";
    }

    // 备课资料提示词：要求依据课程 PPT 生成含固定二级标题的备课内容
    public static String prepare(String lessonTitle, String pptText) {
        return """
                请根据以下课程标题和 PPT 文字生成教师备课资料。
                课程：%s
                PPT 内容：
                %s

                必须包含以下 Markdown 二级标题：
                ## 教学目标
                ## 重点与难点
                ## 知识点提纲
                ## 术语解释
                ## 课堂提问
                ## 随堂练习
                """.formatted(lessonTitle, truncate(pptText, 24000));
    }

    // 课堂学习资料提示词：要求结合 PPT 与课堂字幕生成面向复习的资料
    public static String classMaterial(String lessonTitle, String pptText, String captions) {
        return """
                请根据课程标题、PPT 文字和课堂字幕生成课后学习资料，面向听障学生复习使用。
                课程：%s
                PPT 内容：
                %s
                课堂字幕：
                %s

                必须包含以下 Markdown 二级标题：
                ## 课堂摘要
                ## 重点内容
                ## 术语表
                ## 复习问题
                ## 简单测验
                测验题请附参考答案。
                """.formatted(lessonTitle, truncate(pptText, 12000), truncate(captions, 24000));
    }

    // 视频学习笔记提示词：要求依据视频转写内容生成结构化笔记
    public static String videoNote(String title, String transcript) {
        return """
                请根据视频标题和语音转写内容，为学生生成结构化学习笔记。
                视频：%s
                转写内容：
                %s

                必须包含以下 Markdown 二级标题：
                ## 视频摘要
                ## 内容时间线
                ## 核心概念
                ## 易错点
                ## 复习清单
                """.formatted(title, truncate(transcript, 24000));
    }

    // 将过长文本截断到指定字符数，并附截断说明，避免超出模型上下文
    public static String truncate(String text, int maxChars) {
        String value = text == null ? "" : text.trim();
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "\n\n（内容过长，已截取前 " + maxChars + " 个字符）";
    }
}