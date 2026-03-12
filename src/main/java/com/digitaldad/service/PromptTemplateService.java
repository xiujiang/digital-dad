package com.digitaldad.prompt.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.prompt.dto.*;
import com.digitaldad.prompt.entity.PromptTemplate;
import com.digitaldad.prompt.enums.PromptContentType;
import com.digitaldad.prompt.enums.PromptStatus;
import com.digitaldad.prompt.repository.PromptSceneItemRepository;
import com.digitaldad.prompt.repository.PromptTemplateRepository;
import com.digitaldad.prompt.repository.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提示词模板服务
 * <p>管理提示词模板的增删改查，模板是 prompt 的最小管理单元。</p>
 */
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final PromptTemplateRepository templateRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptSceneItemRepository sceneItemRepository;

    /**
     * 列出模板（支持按 contentType、status 筛选）
     */
    public List<PromptTemplateResponse> listAll(String contentType, String status) {
        List<PromptTemplate> list;
        if (contentType != null && !contentType.isBlank() && status != null && !status.isBlank()) {
            list = templateRepository.findByContentTypeAndStatus(
                    parseContentType(contentType), parseStatus(status));
        } else if (status != null && !status.isBlank()) {
            list = templateRepository.findByStatus(parseStatus(status));
        } else {
            list = templateRepository.findAll();
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 获取模板详情
     */
    public PromptTemplateResponse getById(Long id) {
        PromptTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "模板不存在"));
        return toResponse(t);
    }

    /**
     * 创建模板
     */
    @Transactional
    public PromptTemplateResponse create(CreatePromptTemplateRequest request) {
        if (templateRepository.existsByCode(request.getCode())) {
            throw new BusinessException(400, "编码已存在");
        }
        PromptTemplate t = new PromptTemplate();
        t.setCode(request.getCode());
        t.setName(request.getName());
        t.setContentType(parseContentType(request.getContentType()));
        t.setDescription(request.getDescription());
        t.setStatus(parseStatus(request.getStatus()));
        t = templateRepository.save(t);
        return toResponse(t);
    }

    /**
     * 更新模板
     */
    @Transactional
    public PromptTemplateResponse update(Long id, UpdatePromptTemplateRequest request) {
        PromptTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "模板不存在"));
        if (request.getName() != null) t.setName(request.getName());
        if (request.getContentType() != null) t.setContentType(parseContentType(request.getContentType()));
        if (request.getDescription() != null) t.setDescription(request.getDescription());
        if (request.getStatus() != null) t.setStatus(parseStatus(request.getStatus()));
        t = templateRepository.save(t);
        return toResponse(t);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new BusinessException(404, "模板不存在");
        }
        if (sceneItemRepository.existsByTemplateId(id)) {
            throw new BusinessException(400, "模板已被场景引用，无法删除");
        }
        templateRepository.deleteById(id);
    }

    private PromptTemplateResponse toResponse(PromptTemplate t) {
        Integer activeVersionNo = versionRepository.findByTemplateIdAndIsActiveTrue(t.getId())
                .map(v -> v.getVersionNo())
                .orElse(null);
        return PromptTemplateResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .contentType(t.getContentType().name())
                .description(t.getDescription())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .activeVersionNo(activeVersionNo)
                .build();
    }

    private PromptContentType parseContentType(String s) {
        if (s == null) return PromptContentType.TEXT;
        try {
            return PromptContentType.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PromptContentType.TEXT;
        }
    }

    private PromptStatus parseStatus(String s) {
        if (s == null) return PromptStatus.ENABLED;
        try {
            return PromptStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PromptStatus.ENABLED;
        }
    }
}
