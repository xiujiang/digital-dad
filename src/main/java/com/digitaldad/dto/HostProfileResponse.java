package com.digitaldad.user.dto;

import com.digitaldad.user.enums.ContactVisible;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 主持人我的资料响应
 */
@Data
@Builder
public class HostProfileResponse {

    /** 用户ID */
    private Long userId;

    /** 姓名 */
    private String name;

    /** 电话 */
    private String phone;

    /** 联系方式展示 */
    private ContactVisible contactVisible;

    /** 会员状态：启用/未启用 */
    private Boolean memberEnabled;

    /** 套餐名称 */
    private String packageName;

    /** 套餐总次数 */
    private Integer packageQuota;

    /** 剩余配额 */
    private Integer remainingQuota;

    /** 有效期至 */
    private LocalDateTime validTo;

    /** 账号状态 */
    private String status;
}
