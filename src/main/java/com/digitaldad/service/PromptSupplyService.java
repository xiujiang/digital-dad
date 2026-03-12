package com.digitaldad.prompt.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.prompt.dto.PromptContentDto;
import com.digitaldad.prompt.entity.PromptSceneItem;
import com.digitaldad.prompt.entity.PromptTemplate;
import com.digitaldad.prompt.entity.PromptVersion;
import com.digitaldad.prompt.enums.PromptRoleType;
import com.digitaldad.prompt.enums.PromptSceneScope;
import com.digitaldad.prompt.enums.PromptStatus;
import com.digitaldad.prompt.repository.PromptSceneItemRepository;
import com.digitaldad.prompt.repository.PromptSceneRepository;
import com.digitaldad.prompt.repository.PromptTemplateRepository;
import com.digitaldad.prompt.repository.PromptVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提示词供给服务
 * <p>供会话管理、交付物模块调用，按模板编码或场景编码获取生效的提示词内容。</p>
 */
@Service
@RequiredArgsConstructor
public class PromptSupplyService {

    private final PromptTemplateRepository templateRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptSceneRepository sceneRepository;
    private final PromptSceneItemRepository sceneItemRepository;

    /**
     * 按模板编码获取当前生效的提示词内容
     */
    public String getActiveContentByCode(String templateCode) {
        PromptTemplate t = templateRepository.findByCode(templateCode)
                .orElseThrow(() -> new BusinessException(404, "提示词模板不存在: " + templateCode));
        return versionRepository.findByTemplateIdAndIsActiveTrue(t.getId())
                .map(PromptVersion::getContent)
                .orElseThrow(() -> new BusinessException(400, "模板暂无生效版本: " + templateCode));
    }

    /**
     * 按场景编码获取多个提示词（按顺序，含当前生效版本的 content）
     */
    public List<PromptContentDto> getPromptsBySceneCode(String sceneCode) {
        var scene = sceneRepository.findByCode(sceneCode)
                .orElseThrow(() -> new BusinessException(404, "提示词场景不存在: " + sceneCode));
        if (scene.getStatus() != PromptStatus.ENABLED) {
            throw new BusinessException(400, "场景已停用: " + sceneCode);
        }
        List<PromptSceneItem> items = sceneItemRepository.findBySceneIdOrderByDisplayOrderAsc(scene.getId());
        Map<Long, PromptTemplate> templateMap = new HashMap<>();
        templateRepository.findAllById(items.stream().map(PromptSceneItem::getTemplateId).distinct().collect(Collectors.toList()))
                .forEach(t -> templateMap.put(t.getId(), t));
        Map<Long, PromptVersion> activeVersionMap = new HashMap<>();
        items.forEach(i -> versionRepository.findByTemplateIdAndIsActiveTrue(i.getTemplateId())
                .ifPresent(v -> activeVersionMap.put(i.getTemplateId(), v)));

        return items.stream()
                .filter(i -> activeVersionMap.containsKey(i.getTemplateId()))
                .map(i -> {
                    PromptTemplate t = templateMap.get(i.getTemplateId());
                    PromptVersion v = activeVersionMap.get(i.getTemplateId());
                    return PromptContentDto.builder()
                            .templateId(t.getId())
                            .templateCode(t.getCode())
                            .versionNo(v.getVersionNo())
                            .content(v.getContent())
                            .displayOrder(i.getDisplayOrder())
                            .usageMode(i.getUsageMode().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 按场景编码获取拼接后的完整 content（按 PREPEND 在前、APPEND 在后顺序拼接）
     */
    public String getCombinedContentBySceneCode(String sceneCode) {
        List<PromptContentDto> list = getPromptsBySceneCode(sceneCode);
        StringBuilder sb = new StringBuilder();
        list.stream()
                .filter(p -> !"STANDALONE".equals(p.getUsageMode()))
                .sorted((a, b) -> {
                    int order = Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                    if (order != 0) return order;
                    return "PREPEND".equals(a.getUsageMode()) ? -1 : 1;
                })
                .forEach(p -> {
                    if (sb.length() > 0) sb.append("\n\n");
                    sb.append(p.getContent());
                });
        return sb.toString();
    }

    /**
     * 根据板块+角色组装采访场景编码，如 BOARD_INTERVIEW_FAMILY_ORIGIN_GROOM
     */
    public String buildInterviewSceneCode(String boardCode, PromptRoleType roleType) {
        return "BOARD_INTERVIEW_" + boardCode + "_" + roleType.name();
    }

    /**
     * 获取采访场景提示词
     */
    public List<PromptContentDto> getInterviewPrompts(String boardCode, PromptRoleType roleType) {
        String sceneCode = buildInterviewSceneCode(boardCode, roleType);
        return getPromptsBySceneCode(sceneCode);
    }

    /**
     * 获取小结场景提示词（通用或按板块）
     */
    public List<PromptContentDto> getSummaryPrompts(String boardCode) {
        String sceneCode = boardCode != null && !boardCode.isBlank()
                ? "BOARD_SUMMARY_" + boardCode
                : "BOARD_SUMMARY_COMMON";
        if (sceneRepository.findByCode(sceneCode).isEmpty()) {
            sceneCode = "BOARD_SUMMARY_COMMON";
        }
        return getPromptsBySceneCode(sceneCode);
    }

    /**
     * 获取交付物生成场景提示词
     */
    public List<PromptContentDto> getDeliverablePrompts(String contentType) {
        String sceneCode = "DELIVERABLE_" + contentType;
        return getPromptsBySceneCode(sceneCode);
    }
}
