package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.QuotaFlowResponse;
import com.digitaldad.entity.QuotaFlow;
import com.digitaldad.entity.UserQuota;
import com.digitaldad.enums.FlowType;
import com.digitaldad.enums.QuotaType;
import com.digitaldad.enums.RefType;
import com.digitaldad.repository.QuotaFlowRepository;
import com.digitaldad.repository.UserQuotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户配额服务
 * <p>管理项目、语音转写等配额的查询、校验、扣减、调整及流水记录。</p>
 */
@Service
@RequiredArgsConstructor
public class UserQuotaService {

    private final UserQuotaRepository userQuotaRepository;
    private final QuotaFlowRepository quotaFlowRepository;

    /**
     * 获取剩余配额
     */
    public int getRemaining(Long userId, QuotaType quotaType) {
        return userQuotaRepository.findByUserIdAndQuotaType(userId, quotaType)
                .map(UserQuota::getRemaining)
                .orElse(0);
    }

    /**
     * 校验配额是否充足
     */
    public void checkQuota(Long userId, QuotaType quotaType, int required) {
        int remaining = getRemaining(userId, quotaType);
        if (remaining < required) {
            throw new BusinessException(402, "配额不足，剩余 " + remaining + " 场");
        }
    }

    /**
     * 扣减配额（创建项目时调用）
     */
    @Transactional
    public void deduct(Long userId, QuotaType quotaType, RefType refType, String refId) {
        UserQuota quota = userQuotaRepository.findByUserIdAndQuotaType(userId, quotaType)
                .orElseThrow(() -> new BusinessException(404, "配额记录不存在"));
        if (quota.getRemaining() < 1) {
            throw new BusinessException(402, "配额不足");
        }

        quota.setRemaining(quota.getRemaining() - 1);
        quota.setTotalUsed(quota.getTotalUsed() + 1);
        userQuotaRepository.save(quota);

        QuotaFlow flow = new QuotaFlow();
        flow.setUserId(userId);
        flow.setQuotaType(quotaType);
        flow.setFlowType(FlowType.DEDUCT);
        flow.setDelta(-1);
        flow.setBalanceAfter(quota.getRemaining());
        flow.setReason("创建项目");
        flow.setRefType(refType.name());
        flow.setRefId(refId);
        quotaFlowRepository.save(flow);
    }

    /**
     * 调整配额（超管操作）
     */
    @Transactional
    public void adjust(Long userId, QuotaType quotaType, int delta, String reason, Long operatorId) {
        UserQuota quota = userQuotaRepository.findByUserIdAndQuotaType(userId, quotaType)
                .orElseGet(() -> {
                    UserQuota newQuota = new UserQuota();
                    newQuota.setUserId(userId);
                    newQuota.setQuotaType(quotaType);
                    newQuota.setRemaining(0);
                    newQuota.setTotalUsed(0);
                    return userQuotaRepository.save(newQuota);
                });

        int newRemaining = quota.getRemaining() + delta;
        if (newRemaining < 0) {
            throw new BusinessException(400, "调整后配额不能为负，当前剩余: " + quota.getRemaining());
        }

        quota.setRemaining(newRemaining);
        if (delta < 0) {
            quota.setTotalUsed(quota.getTotalUsed() - delta);
        }
        userQuotaRepository.save(quota);

        FlowType flowType = delta > 0 ? FlowType.RECHARGE : FlowType.ROLLBACK;
        if (delta > 0 && "赠送".equals(reason)) {
            flowType = FlowType.GIFT;
        }

        QuotaFlow flow = new QuotaFlow();
        flow.setUserId(userId);
        flow.setQuotaType(quotaType);
        flow.setFlowType(flowType);
        flow.setDelta(delta);
        flow.setBalanceAfter(newRemaining);
        flow.setReason(reason != null ? reason : "管理员调整");
        flow.setRefType(RefType.ADMIN_ADJUST.name());
        flow.setOperatorId(operatorId);
        quotaFlowRepository.save(flow);
    }

    /**
     * 分页查询配额流水
     */
    public Page<QuotaFlow> getQuotaFlows(Long userId, int page, int size) {
        return quotaFlowRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page - 1, size));
    }

    /**
     * 分页查询配额流水（按类型，返回 DTO）
     */
    public Page<QuotaFlowResponse> getFlowPage(Long userId, QuotaType quotaType, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(50, Math.max(1, size)));
        return quotaFlowRepository.findByUserIdAndQuotaTypeOrderByCreatedAtDesc(userId, quotaType, pageable)
                .map(f -> QuotaFlowResponse.builder()
                        .id(f.getId())
                        .flowType(f.getFlowType().name())
                        .delta(f.getDelta())
                        .balanceAfter(f.getBalanceAfter())
                        .reason(f.getReason())
                        .refType(f.getRefType())
                        .refId(f.getRefId())
                        .createdAt(f.getCreatedAt())
                        .build());
    }
}
