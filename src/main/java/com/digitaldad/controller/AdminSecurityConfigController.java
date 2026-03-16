package com.digitaldad.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.config.dto.PasswordPolicyConfigDto;
import com.digitaldad.entity.SysConfig;
import com.digitaldad.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 超管 - 系统配置（sys_config 表：全部配置查询/修改、密码策略等）
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminSecurityConfigController {

    private final ConfigService configService;

    /**
     * 查询当前全部配置项（sys_config 表）
     */
    @GetMapping
    public Result<List<SysConfig>> listAllConfigs() {
        return Result.ok(configService.listAllConfigs());
    }

    /**
     * 按 configKey 修改配置项，请求体为配置值 JSON（如含 name、各业务字段）
     */
    @PutMapping("/{configKey}")
    public Result<SysConfig> updateConfig(
            @PathVariable String configKey,
            @RequestBody Map<String, Object> configValue) {
        return Result.ok(configService.updateConfig(configKey, configValue));
    }

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
