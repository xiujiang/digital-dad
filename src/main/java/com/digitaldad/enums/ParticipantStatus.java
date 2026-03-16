package com.digitaldad.enums;

/**
 * 项目参与者状态
 */
public enum ParticipantStatus {
    INVITED,    // 已邀请（席位占位，未绑定用户）
    ENTERED,    // 已进入（已绑定，未开始采访）
    IN_PROGRESS,// 采访进行中
    COMPLETED,  // 已完成（四板块全部确认）
    ABANDONED   // 已放弃
}
