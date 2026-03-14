package com.digitaldad.ai.service;

import com.digitaldad.ai.entity.SpeechTranscriptionUsage;
import com.digitaldad.ai.repository.SpeechTranscriptionUsageRepository;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.config.service.ConfigService;
import com.digitaldad.user.entity.UserQuota;
import com.digitaldad.user.enums.QuotaType;
import java.util.Set;
import com.digitaldad.user.repository.UserQuotaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 语音转写配额服务
 * <p>管理用户语音转写配额的初始化、扣减、加额及连接前的预检查。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechTranscriptionQuotaService {

    private static final int BYTES_PER_SECOND = 32000;  // 16k * 16bit * 1ch

    private final UserQuotaRepository userQuotaRepository;
    private final SpeechTranscriptionUsageRepository usageRepository;
    private final ConfigService configService;

    /**
     * 获取或初始化配额（懒加载），返回剩余秒数和累计已使用秒数
     */
    @Transactional
    public SpeechQuotaDto getOrInitQuota(Long userId) {
        UserQuota quota = userQuotaRepository.findByUserIdAndQuotaType(userId, QuotaType.SPEECH_TRANSCRIPTION)
                .orElseGet(() -> initQuota(userId));
        return new SpeechQuotaDto(quota.getRemaining(), quota.getTotalUsed());
    }

    /**
     * 预检查：具备 HOST 或 SUPER_ADMIN 角色直接放行，否则需 remaining > 0
     */
    public void checkQuotaForConnect(Long userId, Set<String> roles) {
        if (roles != null && (roles.contains("HOST") || roles.contains("SUPER_ADMIN"))) {
            return;
        }
        int remaining = getOrInitQuota(userId).remainingSeconds();
        if (remaining <= 0) {
            throw new BusinessException(403, "语音转写额度已用完");
        }
    }

    /**
     * 扣减配额（方案 B：仅当 bytesForwardedToVolcano > 0 时扣减）
     */
    @Transactional
    public void deduct(Long userId, long bytesForwardedToVolcano, String connectId) {
        if (bytesForwardedToVolcano <= 0) {
            return;
        }
        int durationSeconds = (int) (bytesForwardedToVolcano / BYTES_PER_SECOND);
        if (durationSeconds <= 0) {
            return;
        }

        UserQuota quota = userQuotaRepository.findByUserIdAndQuotaTypeForUpdate(userId, QuotaType.SPEECH_TRANSCRIPTION)
                .orElseThrow(() -> new BusinessException(404, "配额记录不存在"));

        int remaining = quota.getRemaining();
        int deducted = Math.min(remaining, durationSeconds);
        if (deducted <= 0) {
            return;
        }

        quota.setRemaining(remaining - deducted);
        quota.setTotalUsed(quota.getTotalUsed() + deducted);
        userQuotaRepository.save(quota);

        SpeechTranscriptionUsage usage = new SpeechTranscriptionUsage();
        usage.setUserId(userId);
        usage.setDurationSeconds(durationSeconds);
        usage.setDeductedSeconds(deducted);
        usage.setRemainingAfter(quota.getRemaining());
        usage.setConnectId(connectId);
        usageRepository.save(usage);

        log.info("语音转写扣减: userId={}, deducted={}s, remaining={}", userId, deducted, quota.getRemaining());
    }

    /**
     * 为指定用户增加配额（超管操作）
     */
    @Transactional
    public void addQuota(Long userId, int addSeconds, Long operatorId) {
        if (addSeconds <= 0) {
            throw new BusinessException(400, "加额数量必须大于 0");
        }
        UserQuota quota = userQuotaRepository.findByUserIdAndQuotaType(userId, QuotaType.SPEECH_TRANSCRIPTION)
                .orElseGet(() -> initQuota(userId));
        quota.setRemaining(quota.getRemaining() + addSeconds);
        userQuotaRepository.save(quota);
        log.info("语音转写加额: userId={}, add={}s, operator={}", userId, addSeconds, operatorId);
    }

    private UserQuota initQuota(Long userId) {
        int defaultSeconds = configService.getSpeechTranscriptionDefaultSeconds();
        UserQuota quota = new UserQuota();
        quota.setUserId(userId);
        quota.setQuotaType(QuotaType.SPEECH_TRANSCRIPTION);
        quota.setRemaining(defaultSeconds);
        quota.setTotalUsed(0);
        return userQuotaRepository.save(quota);
    }

    public record SpeechQuotaDto(int remainingSeconds, int totalUsedSeconds) {}
}
