package com.digitaldad.user.dto;

import com.digitaldad.user.enums.ContactVisible;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新主持人资料请求
 */
@Data
public class UpdateHostProfileRequest {

    /** 姓名，2-20字符 */
    @Size(min = 2, max = 20, message = "姓名长度2-20")
    private String name;

    /** 电话，11位 */
    @Size(min = 11, max = 11, message = "请输入11位手机号")
    private String phone;

    /** 联系方式展示 */
    private ContactVisible contactVisible;
}
