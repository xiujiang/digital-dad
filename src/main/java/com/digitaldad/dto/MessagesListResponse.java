package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 消息列表接口响应（含总轮数）
 */
@Data
@Builder
public class MessagesListResponse {

    /** 消息列表，按序号排序 */
    private List<MessageResponse> messages;
    /** 会话总轮数（已提交的对话轮次） */
    private Integer totalRounds;
}
