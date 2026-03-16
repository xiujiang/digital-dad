package com.digitaldad.service;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.dto.*;
import com.digitaldad.entity.BoardMeta;
import com.digitaldad.enums.BoardMetaStatus;
import com.digitaldad.repository.BoardMetaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 板块元数据服务
 * <p>管理系统的板块元数据（新郎、新娘等），供新建项目时选择板块类型。</p>
 */
@Service
@RequiredArgsConstructor
public class BoardMetaService {

    private final BoardMetaRepository boardMetaRepository;

    /**
     * 列出所有板块元数据
     */
    public List<BoardMetaResponse> listAll() {
        return boardMetaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 列出已启用的板块
     */
    public List<BoardMetaResponse> listEnabled() {
        return boardMetaRepository.findByStatusOrderByDisplayOrderAsc(BoardMetaStatus.ENABLED).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取板块元数据详情
     */
    public BoardMetaResponse getById(Long id) {
        BoardMeta meta = boardMetaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "板块不存在"));
        return toResponse(meta);
    }

    /**
     * 创建板块元数据
     */
    @Transactional
    public BoardMetaResponse create(CreateBoardMetaRequest request) {
        if (boardMetaRepository.existsByCode(request.getCode())) {
            throw new BusinessException(400, "编码已存在");
        }
        BoardMeta meta = new BoardMeta();
        meta.setCode(request.getCode());
        meta.setName(request.getName());
        meta.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        meta.setDescription(request.getDescription());
        meta.setStatus(parseStatus(request.getStatus()));
        meta = boardMetaRepository.save(meta);
        return toResponse(meta);
    }

    /**
     * 更新板块元数据
     */
    @Transactional
    public BoardMetaResponse update(Long id, UpdateBoardMetaRequest request) {
        BoardMeta meta = boardMetaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "板块不存在"));
        if (request.getName() != null) meta.setName(request.getName());
        if (request.getDisplayOrder() != null) meta.setDisplayOrder(request.getDisplayOrder());
        if (request.getDescription() != null) meta.setDescription(request.getDescription());
        if (request.getStatus() != null) meta.setStatus(parseStatus(request.getStatus()));
        meta = boardMetaRepository.save(meta);
        return toResponse(meta);
    }

    /**
     * 删除板块元数据
     */
    @Transactional
    public void delete(Long id) {
        if (!boardMetaRepository.existsById(id)) {
            throw new BusinessException(404, "板块不存在");
        }
        boardMetaRepository.deleteById(id);
    }

    private BoardMetaResponse toResponse(BoardMeta m) {
        return BoardMetaResponse.builder()
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

    private BoardMetaStatus parseStatus(String s) {
        if (s == null) return BoardMetaStatus.ENABLED;
        try {
            return BoardMetaStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BoardMetaStatus.ENABLED;
        }
    }
}
