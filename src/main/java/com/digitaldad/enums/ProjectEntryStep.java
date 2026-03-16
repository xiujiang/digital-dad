package com.digitaldad.enums;

/**
 * C 端用户在当前项目下的入口步骤（用于「我的状态」接口）
 */
public enum ProjectEntryStep {
    /** 未绑定身份，需选择新郎/新娘后 bind */
    NOT_BOUND,
    /** 已绑定，未进会话，需 createOrResume */
    BOUND_NO_SESSION,
    /** 会话中，对话阶段（发消息、提交、生成小结） */
    IN_CHAT,
    /** 当前板块小结待确认 */
    WAITING_SUMMARY_CONFIRM,
    /** 全部板块已完成 */
    ALL_COMPLETED
}
