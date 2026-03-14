package com.digitaldad.board.controller;

import com.digitaldad.board.dto.*;
import com.digitaldad.board.service.BoardMetaService;
import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.common.result.Result;
import com.digitaldad.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 统一板块元数据接口
 * <p>列表：HOST 仅启用，SUPER_ADMIN 全部；增删改仅 SUPER_ADMIN。</p>
 */
@RestController
@RequestMapping("/api/board-meta")
@RequiredArgsConstructor
public class BoardMetaController {

    private final BoardMetaService boardMetaService;

    /**
     * 列表：HOST 仅启用，SUPER_ADMIN 全部
     */
    @GetMapping
    public Result<List<BoardMetaResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.hasRole("SUPER_ADMIN")) {
            return Result.ok(boardMetaService.listAll());
        }
        return Result.ok(boardMetaService.listEnabled());
    }

    /**
     * 已启用的板块列表（兼容：GET /api/board-meta/enabled 与 GET /api/board-meta 对 HOST 行为一致）
     */
    @GetMapping("/enabled")
    public Result<List<BoardMetaResponse>> listEnabled() {
        return Result.ok(boardMetaService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<BoardMetaResponse> get(@PathVariable Long id) {
        return Result.ok(boardMetaService.getById(id));
    }

    @PostMapping
    public Result<BoardMetaResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBoardMetaRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可创建板块元数据");
        }
        return Result.ok(boardMetaService.create(request));
    }

    @PutMapping("/{id}")
    public Result<BoardMetaResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBoardMetaRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可更新板块元数据");
        }
        return Result.ok(boardMetaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可删除板块元数据");
        }
        boardMetaService.delete(id);
        return Result.ok();
    }
}
