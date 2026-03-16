package com.digitaldad.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑交付物请求
 */
@Data
public class UpdateDeliverableRequest {

    @Size(max = 100)
    private String title;

    private String content;
}
