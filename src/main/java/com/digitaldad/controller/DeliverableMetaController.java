package com.digitaldad.controller;

import com.digitaldad.common.exception.BusinessException;
import com.digitaldad.common.result.Result;
import com.digitaldad.dto.*;
import com.digitaldad.security.UserPrincipal;
import com.digitaldad.service.DeliverableMetaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 交付物元数据接口
 * <p>列表：主持人仅返回已启用，超管返回全部；增删改仅超管。</p>
 */
@RestController
@RequestMapping("/api/deliverable-meta")
@RequiredArgsConstructor
public class DeliverableMetaController {

    private final DeliverableMetaService deliverableMetaService;

    /**
     * 列表：主持人仅启用，超管全部
     */
    @GetMapping
    public Result<List<DeliverableMetaResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal.hasRole("SUPER_ADMIN")) {
            return Result.ok(deliverableMetaService.listAll());
        }
        return Result.ok(deliverableMetaService.listEnabled());
    }

    /**
     * 已启用的交付物类型列表（GET /api/deliverable-meta/enabled）
     */
    @GetMapping("/enabled")
    public Result<List<DeliverableMetaResponse>> listEnabled() {
        return Result.ok(deliverableMetaService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<DeliverableMetaResponse> get(@PathVariable Long id) {
        return Result.ok(deliverableMetaService.getById(id));
    }

    @PostMapping
    public Result<DeliverableMetaResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateDeliverableMetaRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可创建交付物元数据");
        }
        return Result.ok(deliverableMetaService.create(request));
    }

    @PutMapping("/{id}")
    public Result<DeliverableMetaResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeliverableMetaRequest request) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可更新交付物元数据");
        }
        return Result.ok(deliverableMetaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        if (!principal.hasRole("SUPER_ADMIN")) {
            throw new BusinessException(403, "仅超管可删除交付物元数据");
        }
        deliverableMetaService.delete(id);
        return Result.ok();
    }
}
