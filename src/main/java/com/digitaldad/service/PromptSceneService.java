package com.digitaldad.prompt.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.prompt.dto.*;
import com.digitaldad.prompt.entity.Prompt;
import com.digitaldad.prompt.entity.PromptScene;
import com.digitaldad.prompt.entity.PromptSceneItem;
import com.digitaldad.prompt.enums.PromptRoleType;
import com.digitaldad.prompt.enums.PromptSceneScope;
import com.digitaldad.prompt.enums.PromptStatus;
import com.digitaldad.prompt.enums.PromptUsageMode;
import com.digitaldad.prompt.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提示词场景服务
 * <p>管理提示词场景的增删改查及场景条目的管理，场景用于将多条提示词按顺序组合成完整提示。</p>
 */
@Service
@RequiredArgsConstructor
public class PromptSceneService {

    private final PromptSceneRepository sceneRepository;
    private final PromptSceneItemRepository sceneItemRepository;
    private final PromptRepository promptRepository;

    /**
     * 列出场景（支持按 scope、boardCode、roleType、status 筛选）
     */
    public List<PromptSceneResponse> listAll(String scope, String boardCode, String roleType, String status) {
        PromptStatus statusEnum = parseStatus(status);
        if (statusEnum == null) statusEnum = PromptStatus.ENABLED;
        List<PromptScene> list;
        if (scope != null && !scope.isBlank()) {
            PromptSceneScope scopeEnum = parseScope(scope);
            if (boardCode != null && !boardCode.isBlank() && roleType != null && !roleType.isBlank()) {
                list = sceneRepository.findByScopeAndBoardCodeAndRoleTypeAndStatus(
                        scopeEnum, boardCode, parseRoleType(roleType), statusEnum);
            } else if (boardCode != null && !boardCode.isBlank()) {
                list = sceneRepository.findByScopeAndBoardCodeAndStatus(scopeEnum, boardCode, statusEnum);
            } else {
                list = sceneRepository.findByScopeAndStatus(scopeEnum, statusEnum);
            }
        } else {
            list = sceneRepository.findByStatus(statusEnum);
        }
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 获取场景详情（含条目列表）
     */
    public PromptSceneResponse getById(Long id) {
        PromptScene s = sceneRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "场景不存在"));
        return toResponseWithItems(s);
    }

    /**
     * 创建场景
     */
    @Transactional
    public PromptSceneResponse create(CreatePromptSceneRequest request) {
        if (sceneRepository.existsByCode(request.getCode())) {
            throw new BusinessException(400, "场景编码已存在");
        }
        PromptScene s = new PromptScene();
        s.setCode(request.getCode());
        s.setName(request.getName());
        s.setScope(parseScope(request.getScope()));
        s.setBoardCode(request.getBoardCode());
        s.setRoleType(request.getRoleType() != null ? parseRoleType(request.getRoleType()) : null);
        s.setDescription(request.getDescription());
        s.setStatus(parseStatus(request.getStatus()));
        s = sceneRepository.save(s);
        return toResponse(s);
    }

    /**
     * 更新场景
     */
    @Transactional
    public PromptSceneResponse update(Long id, UpdatePromptSceneRequest request) {
        PromptScene s = sceneRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "场景不存在"));
        if (request.getName() != null) s.setName(request.getName());
        if (request.getBoardCode() != null) s.setBoardCode(request.getBoardCode());
        if (request.getRoleType() != null) s.setRoleType(parseRoleType(request.getRoleType()));
        if (request.getDescription() != null) s.setDescription(request.getDescription());
        if (request.getStatus() != null) s.setStatus(parseStatus(request.getStatus()));
        s = sceneRepository.save(s);
        return toResponseWithItems(s);
    }

    /**
     * 删除场景
     */
    @Transactional
    public void delete(Long id) {
        if (!sceneRepository.existsById(id)) {
            throw new BusinessException(404, "场景不存在");
        }
        sceneRepository.deleteById(id);
    }

    /**
     * 向场景添加条目（关联提示词 code）
     */
    @Transactional
    public PromptSceneItemResponse addItem(Long sceneId, AddSceneItemRequest request) {
        PromptScene scene = sceneRepository.findById(sceneId)
                .orElseThrow(() -> new BusinessException(404, "场景不存在"));
        if (promptRepository.findByCodeAndIsActiveTrue(request.getPromptCode()).isEmpty()) {
            throw new BusinessException(404, "提示词不存在或未生效: " + request.getPromptCode());
        }
        if (sceneItemRepository.existsBySceneIdAndPromptCode(sceneId, request.getPromptCode())) {
            throw new BusinessException(400, "该提示词已绑定到本场景");
        }
        PromptSceneItem item = new PromptSceneItem();
        item.setSceneId(sceneId);
        item.setPromptCode(request.getPromptCode());
        item.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        item.setUsageMode(parseUsageMode(request.getUsageMode()));
        item = sceneItemRepository.save(item);
        Prompt p = promptRepository.findByCodeAndIsActiveTrue(request.getPromptCode()).orElse(null);
        return toItemResponse(item, p);
    }

    /**
     * 更新场景条目
     */
    @Transactional
    public PromptSceneItemResponse updateItem(Long itemId, UpdateSceneItemRequest request) {
        PromptSceneItem item = sceneItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "场景项不存在"));
        if (request.getDisplayOrder() != null) item.setDisplayOrder(request.getDisplayOrder());
        if (request.getUsageMode() != null) item.setUsageMode(parseUsageMode(request.getUsageMode()));
        item = sceneItemRepository.save(item);
        Prompt p = promptRepository.findByCodeAndIsActiveTrue(item.getPromptCode()).orElse(null);
        return toItemResponse(item, p);
    }

    /**
     * 移除场景条目
     */
    @Transactional
    public void removeItem(Long itemId) {
        if (!sceneItemRepository.existsById(itemId)) {
            throw new BusinessException(404, "场景项不存在");
        }
        sceneItemRepository.deleteById(itemId);
    }

    private PromptSceneResponse toResponse(PromptScene s) {
        return toResponse(s, false);
    }

    private PromptSceneResponse toResponseWithItems(PromptScene s) {
        return toResponse(s, true);
    }

    private PromptSceneResponse toResponse(PromptScene s, boolean withItems) {
        List<PromptSceneItemResponse> items = null;
        if (withItems) {
            List<PromptSceneItem> itemList = sceneItemRepository.findBySceneIdOrderByDisplayOrderAsc(s.getId());
            List<String> codes = itemList.stream().map(PromptSceneItem::getPromptCode).distinct().collect(Collectors.toList());
            Map<String, Prompt> promptMap = promptRepository.findByCodeInAndIsActiveTrue(codes).stream()
                    .collect(Collectors.toMap(Prompt::getCode, p -> p, (a, b) -> a));
            items = itemList.stream()
                    .map(i -> toItemResponse(i, promptMap.get(i.getPromptCode())))
                    .collect(Collectors.toList());
        }
        return PromptSceneResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .scope(s.getScope().name())
                .boardCode(s.getBoardCode())
                .roleType(s.getRoleType() != null ? s.getRoleType().name() : null)
                .description(s.getDescription())
                .status(s.getStatus().name())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .items(items)
                .build();
    }

    private PromptSceneItemResponse toItemResponse(PromptSceneItem i, Prompt p) {
        return PromptSceneItemResponse.builder()
                .id(i.getId())
                .sceneId(i.getSceneId())
                .promptCode(i.getPromptCode())
                .promptName(p != null ? p.getName() : null)
                .displayOrder(i.getDisplayOrder())
                .usageMode(i.getUsageMode().name())
                .build();
    }

    private PromptSceneScope parseScope(String s) {
        try {
            return PromptSceneScope.valueOf(s.toUpperCase());
        } catch (Exception e) {
            throw new BusinessException(400, "无效的 scope");
        }
    }

    private PromptRoleType parseRoleType(String s) {
        try {
            return PromptRoleType.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private PromptStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return PromptStatus.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private PromptUsageMode parseUsageMode(String s) {
        if (s == null) return PromptUsageMode.APPEND;
        try {
            return PromptUsageMode.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return PromptUsageMode.APPEND;
        }
    }
}
