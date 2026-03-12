package com.digitaldad.user.dto;

import lombok.Data;

/**
 * 主持人列表查询参数
 */
@Data
public class AdminHostListRequest {

    /** 页码，从1开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;

    /** 状态：ENABLED/DISABLED */
    private String status;

    /** 配额状态：充足/不足/低余额 */
    private String quotaStatus;

    /** 搜索：姓名或手机号 */
    private String keyword;
}
