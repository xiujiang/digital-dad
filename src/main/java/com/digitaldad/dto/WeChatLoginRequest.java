package com.digitaldad.user.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录请求
 * <p>小程序端 wx.login() 得到 code 后，将 code 发到后端，后端用 code 调微信 code2session 换 openid 并签发 JWT。</p>
 */
@Data
public class WeChatLoginRequest {

    /** 小程序 wx.login() 返回的临时登录凭证 code */
    @NotBlank(message = "code 不能为空")
    private String code;
}
