package com.digitaldad.prompt.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.prompt.dto.*;
import com.digitaldad.prompt.entity.Prompt;
import com.digitaldad.prompt.enums.PromptContentType;
import com.digitaldad.prompt.enums.PromptStatus;
import com.digitaldad.prompt.repository.PromptRepository;
import com.digitaldad.prompt.repository.PromptSceneItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提示词服务（按 code 为逻辑单位，多版本存同一表）
 */
@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository;
    private final PromptSceneItemRepository sceneItemRepository;

    /**
     * 列出当前生效的提示词（每个 code 一条）
     */
    public List<PromptResponse> listAll(String contentType, String status) {
        List<Prompt> list;
        if (contentType != null && !contentType.isBlank() && status != null && !status.isBlank()) {
            list = promptRepository.findByIsActiveTrueAndContentTypeAndStatus(
                    parseContentType(contentType), parseStatus(status));
        } else if (status != null && !status.isBlank()) {
            list = promptRepository.findByIsActiveTrueAndStatus(parseStatus(status));
        } else {
            list = promptRepository.findAllByIsActiveTrue();
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 按 id 获取单行
     */
    public PromptResponse getById(Long id) {
        Prompt p = promptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "提示词不存在"));
        return toResponse(p);
    }

    /**
     * 按 code 获取当前生效行
     */
    public PromptResponse getByCode(String code) {
        Prompt p = promptRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new BusinessException(404, "提示词不存在或未生效: " + code));
        return toResponse(p);
    }

    /**
     * 创建提示词（首版，version_no=1, is_active=true）
     */
    @Transactional
    public PromptResponse create(CreatePromptRequest request) {
        if (promptRepository.existsByCode(request.getCode())) {
            throw new BusinessException(400, "编码已存在");
        }
        Prompt p = new Prompt();
        p.setCode(request.getCode());
        p.setName(request.getName());
        p.setContentType(parseContentType(request.getContentType()));
        p.setDescription(request.getDescription());
        p.setStatus(parseStatus(request.getStatus()));
        p.setVersionNo(1);
        p.setContent(request.getContent());
        p.setIsActive(true);
        p = promptRepository.save(p);
        return toResponse(p);
    }

    /**
     * 更新指定行（按 id）
     */
    @Transactional
    public PromptResponse update(Long id, UpdatePromptRequest request) {
        Prompt p = promptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "提示词不存在"));
        if (request.getName() != null) p.setName(request.getName());
        if (request.getContentType() != null) p.setContentType(parseContentType(request.getContentType()));
        if (request.getDescription() != null) p.setDescription(request.getDescription());
        if (request.getStatus() != null) p.setStatus(parseStatus(request.getStatus()));
        if (request.getContent() != null) p.setContent(request.getContent());
        p = promptRepository.save(p);
        return toResponse(p);
    }

    /**
     * 删除指定行（按 id）；若该 code 被场景引用会禁止删除
     */
    @Transactional
    public void delete(Long id) {
        Prompt p = promptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "提示词不存在"));
        if (sceneItemRepository.existsByPromptCode(p.getCode())) {
            throw new BusinessException(400, "该提示词已被场景引用，无法删除");
        }
        promptRepository.deleteById(id);
    }

    /**
     * 列出某 code 下所有版本
     */
    public List<PromptVersionResponse> listVersionsByCode(String code) {
        return promptRepository.findByCodeOrderByVersionNoDesc(code).stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取版本详情（按版本 id）
     */
    public PromptVersionResponse getVersionById(Long id) {
        Prompt p = promptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "版本不存在"));
        return toVersionResponse(p);
    }

    /**
     * 创建新版本（同 code，version_no 递增）
     */
    @Transactional
    public PromptVersionResponse createVersion(String code, CreatePromptVersionRequest request, Long userId) {
        List<Prompt> existing = promptRepository.findByCodeOrderByVersionNoDesc(code);
        if (existing.isEmpty()) {
            throw new BusinessException(404, "提示词不存在: " + code);
        }
        Prompt first = existing.get(0);
        int nextVersion = existing.stream().mapToInt(Prompt::getVersionNo).max().orElse(0) + 1;

        Prompt p = new Prompt();
        p.setCode(code);
        p.setName(first.getName());
        p.setContentType(first.getContentType());
        p.setDescription(first.getDescription());
        p.setStatus(first.getStatus());
        p.setVersionNo(nextVersion);
        p.setContent(request.getContent());
        p.setIsActive(Boolean.TRUE.equals(request.getSetActive()));
        p.setCreatedBy(userId);
        p = promptRepository.save(p);

        if (p.getIsActive()) {
            promptRepository.deactivateAllByCode(code);
            p.setIsActive(true);
            promptRepository.save(p);
        }
        return toVersionResponse(p);
    }

    /**
     * 激活指定版本（同 code 其他版本置为未生效）
     */
    @Transactional
    public PromptVersionResponse activateVersion(Long versionId) {
        Prompt p = promptRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(404, "版本不存在"));
        promptRepository.deactivateAllByCode(p.getCode());
        p.setIsActive(true);
        p = promptRepository.save(p);
        return toVersionResponse(p);
    }

    /**
     * 删除版本（生效中的不能删）
     */
    @Transactional
    public void deleteVersion(Long id) {
        Prompt p = promptRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "版本不存在"));
        if (Boolean.TRUE.equals(p.getIsActive())) {
            throw new BusinessException(400, "不能删除生效中的版本");
        }
        promptRepository.deleteById(id);
    }

    private PromptResponse toResponse(Prompt p) {
        return PromptResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .contentType(p.getContentType().name())
                .description(p.getDescription())
                .status(p.getStatus().name())
                .versionNo(p.getVersionNo())
                .content(p.getContent())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .createdBy(p.getCreatedBy())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PromptVersionResponse toVersionResponse(Prompt p) {
        return PromptVersionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .versionNo(p.getVersionNo())
                .content(p.getContent())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .createdBy(p.getCreatedBy())
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
