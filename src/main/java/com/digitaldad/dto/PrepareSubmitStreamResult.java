package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 流式提交前置结果：校验通过并标记消息已提交后，用于调用 AI 流式接口的上下文
 */
@Data
@Builder
public class PrepareSubmitStreamResult {

    private String systemPrompt;
    private List<ChatMessage> history;
    private int nextBatch;
}
