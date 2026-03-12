package com.digitaldad.board.service;

import com.digitaldad.board.dto.*;
import com.digitaldad.board.entity.BoardMeta;
import com.digitaldad.board.entity.ProjectBoard;
import com.digitaldad.board.repository.BoardMetaRepository;
import com.digitaldad.board.repository.ProjectBoardRepository;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目板块服务
 * <p>管理项目中板块的增删改查，板块对应采访的不同主题（新郎、新娘等）。</p>
 */
@Service
@RequiredArgsConstructor
public class ProjectBoardService {

    private final ProjectBoardRepository projectBoardRepository;
    private final BoardMetaRepository boardMetaRepository;
    private final ProjectRepository projectRepository;

    /** 校验项目归属：仅主持人可操作自己的项目 */
    private void checkProjectOwnership(Long projectId, Long hostUserId) {
        projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .filter(p -> p.getHostUserId().equals(hostUserId))
                .orElseThrow(() -> new BusinessException(403, "无权限操作该项目"));
    }

    /**
     * 列出项目的所有板块
     */
    public List<ProjectBoardResponse> listByProject(Long projectId, Long hostUserId) {
        checkProjectOwnership(projectId, hostUserId);
        List<ProjectBoard> list = projectBoardRepository.findByProjectIdOrderByDisplayOrderAsc(projectId);
        Map<Long, BoardMeta> metaMap = loadMetaMap(list);
        return list.stream()
                .map(pb -> toResponse(pb, metaMap.get(pb.getBoardMetaId())))
                .collect(Collectors.toList());
    }

    /**
     * 为项目添加板块
     */
    @Transactional
    public ProjectBoardResponse addBoard(Long projectId, Long hostUserId, CreateProjectBoardRequest request) {
        checkProjectOwnership(projectId, hostUserId);
        BoardMeta meta = boardMetaRepository.findById(request.getBoardMetaId())
                .orElseThrow(() -> new BusinessException(404, "板块不存在"));
        if (projectBoardRepository.existsByProjectIdAndBoardMetaId(projectId, request.getBoardMetaId())) {
            throw new BusinessException(400, "该项目已包含该板块");
        }
        ProjectBoard pb = new ProjectBoard();
        pb.setProjectId(projectId);
        pb.setBoardMetaId(request.getBoardMetaId());
        pb.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        pb = projectBoardRepository.save(pb);
        return toResponse(pb, meta);
    }

    /**
     * 更新板块
     */
    @Transactional
    public ProjectBoardResponse update(Long projectId, Long projectBoardId, Long hostUserId, UpdateProjectBoardRequest request) {
        checkProjectOwnership(projectId, hostUserId);
        ProjectBoard pb = projectBoardRepository.findById(projectBoardId)
                .orElseThrow(() -> new BusinessException(404, "项目板块不存在"));
        if (!pb.getProjectId().equals(projectId)) {
            throw new BusinessException(400, "项目板块不归属于该项目");
        }
        if (request.getDisplayOrder() != null) pb.setDisplayOrder(request.getDisplayOrder());
        pb = projectBoardRepository.save(pb);
        BoardMeta meta = boardMetaRepository.findById(pb.getBoardMetaId()).orElse(null);
        return toResponse(pb, meta);
    }

    /**
     * 删除板块
     */
    @Transactional
    public void remove(Long projectId, Long projectBoardId, Long hostUserId) {
        checkProjectOwnership(projectId, hostUserId);
        ProjectBoard pb = projectBoardRepository.findById(projectBoardId)
                .orElseThrow(() -> new BusinessException(404, "项目板块不存在"));
        if (!pb.getProjectId().equals(projectId)) {
            throw new BusinessException(400, "项目板块不归属于该项目");
        }
        projectBoardRepository.deleteById(projectBoardId);
    }

    private Map<Long, BoardMeta> loadMetaMap(List<ProjectBoard> list) {
        List<Long> ids = list.stream().map(ProjectBoard::getBoardMetaId).distinct().collect(Collectors.toList());
        Map<Long, BoardMeta> map = new HashMap<>();
        boardMetaRepository.findAllById(ids).forEach(m -> map.put(m.getId(), m));
        return map;
    }

    private ProjectBoardResponse toResponse(ProjectBoard pb, BoardMeta meta) {
        return ProjectBoardResponse.builder()
                .id(pb.getId())
                .projectId(pb.getProjectId())
                .boardMetaId(pb.getBoardMetaId())
                .boardCode(meta != null ? meta.getCode() : null)
                .boardName(meta != null ? meta.getName() : null)
                .displayOrder(pb.getDisplayOrder())
                .createdAt(pb.getCreatedAt())
                .build();
    }
}
