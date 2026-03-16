package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.*;
import com.digitaldad.entity.DeliverableMeta;
import com.digitaldad.enums.DeliverableMetaStatus;
import com.digitaldad.repository.DeliverableMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 交付物元数据服务
 * <p>管理可生成的交付物类型配置，供主持人端展示列表。</p>
 */
@Service
@RequiredArgsConstructor
public class DeliverableMetaService {

    private final DeliverableMetaRepository deliverableMetaRepository;

    /**
     * 列出所有交付物元数据
     */
    public List<DeliverableMetaResponse> listAll() {
        return deliverableMetaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 列出已启用的交付物类型（按 display_order 排序）
     */
    public List<DeliverableMetaResponse> listEnabled() {
        return deliverableMetaRepository.findByStatusOrderByDisplayOrderAsc(DeliverableMetaStatus.ENABLED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 获取交付物元数据
     */
    public DeliverableMetaResponse getById(Long id) {
        DeliverableMeta meta = deliverableMetaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "交付物类型不存在"));
        return toResponse(meta);
    }

    /**
     * 创建交付物元数据
     */
    @Transactional
    public DeliverableMetaResponse create(CreateDeliverableMetaRequest request) {
        if (deliverableMetaRepository.existsByCode(request.getCode())) {
            throw new BusinessException(400, "编码已存在");
        }
        DeliverableMeta meta = new DeliverableMeta();
        meta.setCode(request.getCode());
        meta.setName(request.getName());
        meta.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        meta.setDescription(request.getDescription());
        meta.setStatus(parseStatus(request.getStatus()));
        meta = deliverableMetaRepository.save(meta);
        return toResponse(meta);
    }

    /**
     * 更新交付物元数据
     */
    @Transactional
    public DeliverableMetaResponse update(Long id, UpdateDeliverableMetaRequest request) {
        DeliverableMeta meta = deliverableMetaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "交付物类型不存在"));
        if (request.getName() != null) meta.setName(request.getName());
        if (request.getDisplayOrder() != null) meta.setDisplayOrder(request.getDisplayOrder());
        if (request.getDescription() != null) meta.setDescription(request.getDescription());
        if (request.getStatus() != null) meta.setStatus(parseStatus(request.getStatus()));
        meta = deliverableMetaRepository.save(meta);
        return toResponse(meta);
    }

    /**
     * 删除交付物元数据
     */
    @Transactional
    public void delete(Long id) {
        if (!deliverableMetaRepository.existsById(id)) {
            throw new BusinessException(404, "交付物类型不存在");
        }
        deliverableMetaRepository.deleteById(id);
    }

    private DeliverableMetaResponse toResponse(DeliverableMeta m) {
        return DeliverableMetaResponse.builder()
                .id(m.getId())
                .code(m.getCode())
                .name(m.getName())
                .displayOrder(m.getDisplayOrder())
                .description(m.getDescription())
                .status(m.getStatus().name())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private DeliverableMetaStatus parseStatus(String s) {
        if (s == null) return DeliverableMetaStatus.ENABLED;
        try {
            return DeliverableMetaStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DeliverableMetaStatus.ENABLED;
        }
    }
}
