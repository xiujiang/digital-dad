package com.digitaldad.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新消息请求
 */
@Data
public class UpdateMessageRequest {

    @Size(max = 5000)
    private String content;
}
