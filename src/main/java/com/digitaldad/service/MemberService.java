package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.MemberPackageConfigDto;
import com.digitaldad.service.ConfigService;
import com.digitaldad.entity.User;
import com.digitaldad.entity.UserMember;
import com.digitaldad.enums.MemberStatus;
import com.digitaldad.enums.QuotaType;
import com.digitaldad.repository.UserMemberRepository;
import com.digitaldad.repository.UserRepository;
import com.digitaldad.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 会员服务
 * <p>为主持人开通会员套餐，配置从 sys_config 的 member.packages 读取，开通后增加项目配额。</p>
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final String MEMBER_TYPE_HOST = "HOST";
    private static final String ROLE_HOST = "HOST";

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMemberRepository userMemberRepository;
    private final UserQuotaService userQuotaService;
    private final ConfigService configService;

    /**
     * 为指定用户开通会员
     *
     * @param userId     用户ID（主持人）
     * @param packageCode 套餐编码，如 annual、single（从 sys_config 的 member.packages 读取）
     */
    @Transactional
    public void activateMembership(Long userId, String packageCode) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        if (!userRoleRepository.existsByUserIdAndRole(userId, ROLE_HOST)) {
            throw new BusinessException(400, "仅支持为主持人开通会员");
        }

        MemberPackageConfigDto config = configService.getMemberPackageConfig(packageCode);
        if (config.getQuota() == null || config.getQuota() < 1) {
            throw new BusinessException(500, "套餐配额配置异常");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validTo = config.getValidDays() != null && config.getValidDays() > 0
                ? now.plusDays(config.getValidDays())
                : null;

        UserMember member = userMemberRepository.findByUserIdAndMemberType(userId, MEMBER_TYPE_HOST)
                .orElseGet(() -> {
                    UserMember m = new UserMember();
                    m.setUserId(userId);
                    m.setMemberType(MEMBER_TYPE_HOST);
                    return m;
                });

        member.setPackageName(config.getName());
        member.setPackageQuota(config.getQuota());
        member.setValidFrom(now);
        member.setValidTo(validTo);
        member.setStatus(MemberStatus.ACTIVE);
        userMemberRepository.save(member);

        userQuotaService.adjust(userId, QuotaType.PROJECT, config.getQuota(),
                "开通" + config.getName(), null);
    }
}
