package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参与者在某板块下的故事（项目详情-参与者维度，用于展示故事而非小结快照）
 */
@Data
@Builder
public class ParticipantBoardStoryResponse {

    private Long projectBoardId;
    private String boardCode;
    private String boardName;
    private Integer displayOrder;

    private Long id;           // BoardStory.id
    private String content;    // 故事正文
    private Integer versionNo;
    private LocalDateTime createdAt;
}
