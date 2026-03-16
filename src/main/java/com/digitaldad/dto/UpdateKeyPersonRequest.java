package com.digitaldad.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新关键人物请求
 */
@Data
public class UpdateKeyPersonRequest {

    @Size(max = 50)
    private String name;

    @Size(max = 50)
    private String roleLabel;
}
