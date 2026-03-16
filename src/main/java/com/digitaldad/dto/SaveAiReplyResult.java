package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 流式提交结束后落库 AI 消息的返回结果
 */
@Data
@Builder
public class SaveAiReplyResult {

    private Long messageId;
    private Integer roundCount;
}
