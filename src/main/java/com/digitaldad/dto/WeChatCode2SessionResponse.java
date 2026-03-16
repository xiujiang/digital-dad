package com.digitaldad.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信 jscode2session 接口响应
 * <p>成功时含 openid、session_key；失败时含 errcode、errmsg。</p>
 */
@Data
public class WeChatCode2SessionResponse {

    /** 用户唯一标识（成功时返回） */
    private String openid;

    /** 会话密钥（成功时返回，用于解密等） */
    @JsonProperty("session_key")
    private String sessionKey;

    /** 用户在开放平台的唯一标识（成功且满足条件时返回） */
    private String unionid;

    /** 错误码（失败时返回，0 表示成功） */
    private Integer errcode;

    /** 错误信息（失败时返回） */
    private String errmsg;
}
