package com.digitaldad.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.config.dto.PasswordPolicyConfigDto;
import com.digitaldad.config.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 超管 - 安全设置（密码策略等）
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminSecurityConfigController {

    private final ConfigService configService;

    /**
     * 获取密码策略配置
     */
    @GetMapping("/password-policy")
    public Result<PasswordPolicyConfigDto> getPasswordPolicy() {
        return Result.ok(configService.getPasswordPolicy());
    }

    /**
     * 更新密码策略配置
     */
    @PutMapping("/password-policy")
    public Result<PasswordPolicyConfigDto> updatePasswordPolicy(
            @Valid @RequestBody PasswordPolicyConfigDto request) {
        return Result.ok(configService.updatePasswordPolicy(request));
    }
}
