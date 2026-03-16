package com.digitaldad.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 分享入口响应
 */
@Data
@Builder
public class ShareEntryResponse {

    /** 分享链接 */
    private String shareUrl;

    /** 入口 URL（C 端扫码进入） */
    private String entryUrl;

    /** 二维码图片 URL */
    private String qrCodeUrl;

    /** 分享令牌（用于二维码内容） */
    private String shareToken;

    /** 项目ID */
    private Long projectId;

    /** 微信小程序 URL Scheme（weixin://dl/business/?t=xxx），用于分享到微信内打开小程序；未配置或生成失败时为 null */
    private String wechatScheme;

    /** 基于 wechatScheme 生成的二维码图片 URL，便于下载后发微信；无 scheme 时为 null */
    private String wechatSchemeQrCodeUrl;
}
