package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.PromptContentDto;
import com.digitaldad.entity.Prompt;
import com.digitaldad.entity.PromptSceneItem;
import com.digitaldad.enums.PromptRoleType;
import com.digitaldad.enums.PromptStatus;
import com.digitaldad.repository.PromptRepository;
import com.digitaldad.repository.PromptSceneItemRepository;
import com.digitaldad.repository.PromptSceneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提示词供给服务
 * <p>按场景编码获取生效的提示词内容，供交付物、小结、访谈等调用。</p>
 */
@Service
@RequiredArgsConstructor
public class PromptSupplyService {

    private final PromptRepository promptRepository;
    private final PromptSceneRepository sceneRepository;
    private final PromptSceneItemRepository sceneItemRepository;

    /**
     * 按提示词编码获取当前生效的正文
     */
    public String getActiveContentByCode(String promptCode) {
        Prompt p = promptRepository.findByCodeAndIsActiveTrue(promptCode)
                .orElseThrow(() -> new BusinessException(404, "提示词不存在或未生效: " + promptCode));
        return p.getContent();
    }

    /**
     * 按场景编码获取多个提示词（按顺序，含当前生效的 content）
     */
    public List<PromptContentDto> getPromptsBySceneCode(String sceneCode) {
        var scene = sceneRepository.findByCode(sceneCode)
                .orElseThrow(() -> new BusinessException(404, "提示词场景不存在: " + sceneCode));
        if (scene.getStatus() != PromptStatus.ENABLED) {
            throw new BusinessException(400, "场景已停用: " + sceneCode);
        }
        List<PromptSceneItem> items = sceneItemRepository.findBySceneIdOrderByDisplayOrderAsc(scene.getId());
        List<String> codes = items.stream().map(PromptSceneItem::getPromptCode).distinct().collect(Collectors.toList());
        Map<String, Prompt> promptMap = promptRepository.findByCodeInAndIsActiveTrue(codes).stream()
                .collect(Collectors.toMap(Prompt::getCode, p -> p, (a, b) -> a));

        return items.stream()
                .filter(i -> promptMap.containsKey(i.getPromptCode()))
                .map(i -> {
                    Prompt p = promptMap.get(i.getPromptCode());
                    return PromptContentDto.builder()
                            .promptId(p.getId())
                            .promptCode(p.getCode())
                            .versionNo(p.getVersionNo())
                            .content(p.getContent())
                            .displayOrder(i.getDisplayOrder())
                            .usageMode(i.getUsageMode().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 按场景编码获取拼接后的完整 content
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
     * 根据板块+角色组装采访场景编码
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
     * 获取小结场景提示词（通用或按板块）。
     * 小结只发「小结提示词」、不发访谈角色/如何问答，避免模型输出叙述而非 JSON。
     * 仅保留 code=BOARD_SUMMARY_COMMON_PROMPT 的提示词。
     */
    public List<PromptContentDto> getSummaryPrompts(String boardCode) {
        String sceneCode = boardCode != null && !boardCode.isBlank()
                ? "BOARD_SUMMARY_" + boardCode
                : "BOARD_SUMMARY_COMMON";
        if (sceneRepository.findByCode(sceneCode).isEmpty()) {
            sceneCode = "BOARD_SUMMARY_COMMON";
        }
        return getPromptsBySceneCode(sceneCode).stream()
                .filter(p -> "BOARD_SUMMARY_COMMON_PROMPT".equals(p.getPromptCode()))
                .collect(Collectors.toList());
    }

    /**
     * 获取故事生成场景提示词（通用或按板块）
     */
    public List<PromptContentDto> getStoryPrompts(String boardCode) {
        String sceneCode = boardCode != null && !boardCode.isBlank()
                ? "BOARD_STORY_" + boardCode
                : "BOARD_STORY_COMMON";
        if (sceneRepository.findByCode(sceneCode).isEmpty()) {
            sceneCode = "BOARD_STORY_COMMON";
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
