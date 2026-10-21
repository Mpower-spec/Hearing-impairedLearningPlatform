package com.hearing.platform.ai;

/**
 * AI 调用结果封装。
 *
 * @param content 生成的文本内容
 * @param source  来源标记：AI 表示大模型/ASR 真实生成，MOCK 表示降级模拟内容
 */
public record AiResult(String content, String source) {
}
