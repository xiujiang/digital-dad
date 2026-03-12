package com.digitaldad.user.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.user.dto.HostProfileResponse;
import com.digitaldad.user.dto.UpdateHostProfileRequest;
import com.digitaldad.user.entity.User;
import com.digitaldad.user.entity.UserMember;
import com.digitaldad.user.entity.UserQuota;
import com.digitaldad.user.enums.MemberStatus;
import com.digitaldad.user.enums.QuotaType;
import com.digitaldad.user.enums.UserType;
import com.digitaldad.user.repository.UserMemberRepository;
import com.digitaldad.user.repository.UserQuotaRepository;
import com.digitaldad.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 主持人用户服务
 * <p>提供主持人资料的查询与更新，含会员、配额等信息。</p>
 */
@Service
@RequiredArgsConstructor
public class HostUserService {

    private final UserRepository userRepository;
    private final UserMemberRepository userMemberRepository;
    private final UserQuotaRepository userQuotaRepository;

    /**
     * 获取主持人我的资料（含会员、配额）
     */
    public HostProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (user.getUserType() != UserType.HOST) {
            throw new BusinessException(403, "非主持人账号");
        }

        int remainingQuota = 0;
        String packageName = null;
        LocalDateTime validTo = null;
        boolean memberEnabled = false;

        Optional<UserQuota> quotaOpt = userQuotaRepository.findByUserIdAndQuotaType(userId, QuotaType.PROJECT);
        if (quotaOpt.isPresent()) {
            remainingQuota = quotaOpt.get().getRemaining();
        }

        Optional<UserMember> memberOpt = userMemberRepository.findByUserIdAndMemberType(userId, "HOST");
        if (memberOpt.isPresent()) {
            UserMember m = memberOpt.get();
            packageName = m.getPackageName();
            validTo = m.getValidTo();
            memberEnabled = m.getStatus() == MemberStatus.ACTIVE
                    && (m.getValidTo() == null || m.getValidTo().isAfter(LocalDateTime.now()));
        } else {
            // 无套餐时，有配额即视为启用
            memberEnabled = remainingQuota > 0;
        }

        return HostProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .contactVisible(user.getContactVisible())
                .memberEnabled(memberEnabled)
                .packageName(packageName)
                .remainingQuota(remainingQuota)
                .validTo(validTo)
                .status(user.getStatus().name())
                .build();
    }

    /**
     * 更新主持人资料
     */
    @Transactional
    public void updateProfile(Long userId, UpdateHostProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (user.getUserType() != UserType.HOST) {
            throw new BusinessException(403, "非主持人账号");
        }

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getContactVisible() != null) {
            user.setContactVisible(request.getContactVisible());
        }
        userRepository.save(user);
    }
}
