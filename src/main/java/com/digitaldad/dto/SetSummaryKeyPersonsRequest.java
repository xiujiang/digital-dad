package com.digitaldad.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 设置小结绑定的关键人物
 */
@Data
public class SetSummaryKeyPersonsRequest {

    @NotNull(message = "关键人物ID列表不能为null")
    private List<Long> keyPersonIds;
}
