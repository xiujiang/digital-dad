package com.digitaldad.project.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息响应
 */
@Data
@Builder
public class MessageResponse {

    private Long id;
    private Long sessionId;
    private String senderType;
    private String messageType;
    private String content;
    private String audioUrl;
    private Integer sequenceNo;
    private Integer batchNo;
    private Boolean isSubmitted;
    private LocalDateTime createdAt;
}
