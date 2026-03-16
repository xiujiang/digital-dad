package com.digitaldad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录/注册请求
 * <p>前端 wx.login() 得到 code 后传给我方后端；后端用 code 调微信 jscode2session 换 openid，再查/建用户并签发 JWT。</p>
 */
@Data
public class WeChatLoginRequest {

    /** 微信临时登录凭证（前端 wx.login() 获得），后端用其调 jscode2session 换 openid */
    @NotBlank(message = "code 不能为空")
    private String code;

    /** 微信 unionid（可选，若前端有可传；也可由 code2session 返回后后端写入） */
    private String unionId;

    /** 用户昵称（来自小程序用户信息） */
    private String nickName;

    /** 用户头像 URL（来自小程序用户信息） */
    private String avatarUrl;

    /** 性别：0 未知，1 男，2 女 */
    private Integer gender;

    /** 国家 */
    private String country;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;
}
