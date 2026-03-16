package com.digitaldad.controller;

import com.digitaldad.service.SpeechTranscriptionQuotaService;
import com.digitaldad.common.result.Result;
import com.digitaldad.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 超管 - 语音转写配额管理
 * <p>提供管理员为指定用户增加语音转写配额的功能，用于延长用户语音识别的可用时长。</p>
 */
@RestController
@RequestMapping("/api/admin/speech-quota")
@RequiredArgsConstructor
public class AdminSpeechQuotaController {

    private final SpeechTranscriptionQuotaService quotaService;

    /**
     * 为指定用户增加语音转写配额
     *
     * @param principal 当前登录的超管用户
     * @param request   加额请求，包含目标用户ID和增加的秒数
     * @return 成功时返回空结果
     */
    @PostMapping("/add")
    public Result<Void> addQuota(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddSpeechQuotaRequest request) {
        quotaService.addQuota(request.getUserId(), request.getAddSeconds(), principal.getUserId());
        return Result.ok();
    }

    @Data
    public static class AddSpeechQuotaRequest {
        @NotNull(message = "用户ID不能为空")
        private Long userId;

        @NotNull(message = "加额秒数不能为空")
        @Min(value = 1, message = "加额秒数必须大于0")
        private Integer addSeconds;
    }
}
