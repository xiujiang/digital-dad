package com.digitaldad.prompt.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.prompt.dto.*;
import com.digitaldad.prompt.entity.PromptTemplate;
import com.digitaldad.prompt.entity.PromptVersion;
import com.digitaldad.prompt.repository.PromptTemplateRepository;
import com.digitaldad.prompt.repository.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提示词版本服务
 * <p>管理模板的版本创建、激活、删除，同一模板仅一个版本生效。</p>
 */
@Service
@RequiredArgsConstructor
public class PromptVersionService {

    private final PromptTemplateRepository templateRepository;
    private final PromptVersionRepository versionRepository;

    /**
     * 列出模板下的所有版本
     */
    public List<PromptVersionResponse> listByTemplate(Long templateId) {
        return versionRepository.findByTemplateIdOrderByVersionNoDesc(templateId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取版本详情
     */
    public PromptVersionResponse getById(Long id) {
        PromptVersion v = versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "版本不存在"));
        return toResponse(v);
    }

    /**
     * 创建新版本
     */
    @Transactional
    public PromptVersionResponse create(Long templateId, CreatePromptVersionRequest request, Long userId) {
        PromptTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(404, "模板不存在"));

        int nextVersion = versionRepository.findByTemplateIdOrderByVersionNoDesc(templateId).stream()
                .mapToInt(PromptVersion::getVersionNo)
                .max()
                .orElse(0) + 1;

        PromptVersion v = new PromptVersion();
        v.setTemplateId(templateId);
        v.setVersionNo(nextVersion);
        v.setContent(request.getContent());
        v.setIsActive(Boolean.TRUE.equals(request.getSetActive()));
        v.setCreatedBy(userId);
        v = versionRepository.save(v);

        if (v.getIsActive()) {
            versionRepository.deactivateAllByTemplateId(templateId);
            v.setIsActive(true);
            versionRepository.save(v);
        }

        return toResponse(v);
    }

    /**
     * 激活指定版本（使其他版本失效）
     */
    @Transactional
    public PromptVersionResponse activate(Long versionId) {
        PromptVersion v = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(404, "版本不存在"));
        versionRepository.deactivateAllByTemplateId(v.getTemplateId());
        v.setIsActive(true);
        v = versionRepository.save(v);
        return toResponse(v);
    }

    /**
     * 删除版本（生效中的不能删）
     */
    @Transactional
    public void delete(Long id) {
        PromptVersion v = versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "版本不存在"));
        if (v.getIsActive()) {
            throw new BusinessException(400, "不能删除生效中的版本");
        }
        versionRepository.deleteById(id);
    }

    private PromptVersionResponse toResponse(PromptVersion v) {
        return PromptVersionResponse.builder()
                .id(v.getId())
                .templateId(v.getTemplateId())
                .versionNo(v.getVersionNo())
                .content(v.getContent())
                .isActive(v.getIsActive())
                .createdAt(v.getCreatedAt())
                .createdBy(v.getCreatedBy())
                .build();
    }
}
