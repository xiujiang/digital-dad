package com.digitaldad.project.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送消息请求
 */
@Data
public class SendMessageRequest {

    @Size(max = 5000)
    private String content;

    private String audioUrl;

    private String transcriptText;
}
