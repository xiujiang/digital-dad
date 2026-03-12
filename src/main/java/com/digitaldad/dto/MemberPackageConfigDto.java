package com.digitaldad.config.dto;

import lombok.Data;

/**
 * 会员套餐配置（从 sys_config 的 member.packages 解析）
 */
@Data
public class MemberPackageConfigDto {

    /** 套餐名称 */
    private String name;

    /** 配额次数 */
    private Integer quota;

    /** 有效天数，null 表示无时间限制 */
    private Integer validDays;
}
