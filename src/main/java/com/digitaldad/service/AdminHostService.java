package com.digitaldad.user.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.user.dto.*;
import com.digitaldad.user.entity.User;
import com.digitaldad.user.entity.UserQuota;
import com.digitaldad.user.enums.QuotaType;
import com.digitaldad.user.enums.UserStatus;
import com.digitaldad.user.enums.UserType;
import com.digitaldad.user.repository.UserQuotaRepository;
import com.digitaldad.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 超管 - 主持人管理服务
 * <p>提供主持人列表、创建、详情、状态更新、配额调整及流水查询。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminHostService {

    private final UserRepository userRepository;
    private final UserQuotaRepository userQuotaRepository;
    private final UserQuotaService userQuotaService;

    /**
     * 主持人列表（分页、筛选）
     */
    public Page<AdminHostListItemResponse> listHosts(AdminHostListRequest request) {
        UserStatus status = parseStatus(request.getStatus());
        String keyword = request.getKeyword();
        Pageable pageable = PageRequest.of(
                Math.max(0, request.getPage() - 1),
                Math.min(50, Math.max(1, request.getSize())),
                Sort.by(Sort.Direction.DESC, "lastLoginAt"));

        Page<User> page = userRepository.findByUserTypeAndFilters(UserType.HOST, status, keyword, pageable);

        return page.map(user -> {
            int remaining = userQuotaRepository.findByUserIdAndQuotaType(user.getId(), QuotaType.PROJECT)
                    .map(UserQuota::getRemaining)
                    .orElse(0);
            return AdminHostListItemResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .phone(user.getPhone())
                    .status(user.getStatus().name())
                    .quotaRemaining(remaining)
                    .lastLoginAt(user.getLastLoginAt())
                    .createdAt(user.getCreatedAt())
                    .build();
        });
    }

    /**
     * 新增主持人
     */
    @Transactional
    public AdminHostDetailResponse createHost(AdminCreateHostRequest request) {
        if (userRepository.existsByPhoneAndDeletedAtIsNull(request.getPhone())) {
            throw new BusinessException(400, "该手机号已注册");
        }

        User user = new User();
        user.setUserType(UserType.HOST);
        user.setStatus(UserStatus.ENABLED);
        user.setPhone(request.getPhone());
        user.setName(request.getName());
        user = userRepository.save(user);

        // 初始化配额（始终创建 UserQuota 记录，便于后续扣减）
        int initialQuota = request.getInitialQuota() != null ? Math.max(0, request.getInitialQuota()) : 0;
        userQuotaService.adjust(user.getId(), QuotaType.PROJECT, initialQuota, "初始配额", null);

        return getHostDetail(user.getId());
    }

    /**
     * 主持人详情
     */
    public AdminHostDetailResponse getHostDetail(Long hostId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(hostId)
                .orElseThrow(() -> new BusinessException(404, "主持人不存在"));
        if (user.getUserType() != UserType.HOST) {
            throw new BusinessException(400, "非主持人账号");
        }

        int remaining = 0;
        int totalUsed = 0;
        Optional<UserQuota> quotaOpt = userQuotaRepository.findByUserIdAndQuotaType(hostId, QuotaType.PROJECT);
        if (quotaOpt.isPresent()) {
            remaining = quotaOpt.get().getRemaining();
            totalUsed = quotaOpt.get().getTotalUsed();
        }

        return AdminHostDetailResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .status(user.getStatus().name())
                .quotaRemaining(remaining)
                .quotaTotalUsed(totalUsed)
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 启用/停用主持人
     */
    @Transactional
    public void updateHostStatus(Long hostId, AdminUpdateHostStatusRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(hostId)
                .orElseThrow(() -> new BusinessException(404, "主持人不存在"));
        if (user.getUserType() != UserType.HOST) {
            throw new BusinessException(400, "非主持人账号");
        }

        UserStatus newStatus = UserStatus.valueOf(request.getStatus().toUpperCase());
        user.setStatus(newStatus);
        userRepository.save(user);
    }

    /**
     * 调整配额
     */
    @Transactional
    public void adjustQuota(Long hostId, AdminAdjustQuotaRequest request, Long operatorId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(hostId)
                .orElseThrow(() -> new BusinessException(404, "主持人不存在"));
        if (user.getUserType() != UserType.HOST) {
            throw new BusinessException(400, "非主持人账号");
        }

        userQuotaService.adjust(hostId, QuotaType.PROJECT, request.getDelta(),
                request.getReason() != null ? request.getReason() : "管理员调整", operatorId);
    }

    /**
     * 配额流水
     */
    public Page<QuotaFlowResponse> getQuotaFlows(Long hostId, int page, int size) {
        User user = userRepository.findByIdAndDeletedAtIsNull(hostId)
                .orElseThrow(() -> new BusinessException(404, "主持人不存在"));
        if (user.getUserType() != UserType.HOST) {
            throw new BusinessException(400, "非主持人账号");
        }

        return userQuotaService.getFlowPage(hostId, QuotaType.PROJECT, page, size);
    }

    private UserStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return null;
        }
        try {
            return UserStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
