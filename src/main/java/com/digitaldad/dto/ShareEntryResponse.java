package com.digitaldad.project.dto;

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
}
