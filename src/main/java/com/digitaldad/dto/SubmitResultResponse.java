package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 提交给AI后的结果
 */
@Data
@Builder
public class SubmitResultResponse {

    private Integer newBatchNo;
    /** 会话累计已完成轮数 */
    private Integer roundCount;
    /** 当前板块最大轮数（配置） */
    private Integer maxRoundsPerBoard;
    private List<MessageResponse> newMessages;
}
