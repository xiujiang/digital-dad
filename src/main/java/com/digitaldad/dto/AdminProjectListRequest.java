package com.digitaldad.project.dto;

import lombok.Data;

/**
 * 管理员 - 项目列表查询参数
 */
@Data
public class AdminProjectListRequest {

    /** 页码，从1开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;

    /** 主持人ID筛选（不传则查全部） */
    private Long hostUserId;

    /** 项目状态筛选 */
    private String status;

    /** 关键词：新人姓名、项目编号 */
    private String keyword;
}
