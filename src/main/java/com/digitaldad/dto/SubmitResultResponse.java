package com.digitaldad.project.dto;

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
    private Integer roundCount;
    private List<MessageResponse> newMessages;
}
