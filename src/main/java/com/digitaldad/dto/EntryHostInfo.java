package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

/**
 * C 端入口页展示的主持人信息
 */
@Data
@Builder
public class EntryHostInfo {

    /** 主持人姓名（昵称） */
    private String name;

    /** 主持人手机号（可选，用于联系主持人） */
    private String phone;
}
