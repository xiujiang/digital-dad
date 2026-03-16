package com.digitaldad.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.dto.AdminDeliverableListItemResponse;
import com.digitaldad.service.DeliverableService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 超管 - 交付物管理（内容列表）
 * <p>全量交付物列表，展示主持人生成信息，仅 SUPER_ADMIN 可访问。</p>
 */
@RestController
@RequestMapping("/api/admin/deliverables")
@RequiredArgsConstructor
public class AdminDeliverableController {

    private final DeliverableService deliverableService;

    /**
     * 分页列出全部交付物（含主持人生成人/作者）
     */
    @GetMapping
    public Result<Page<AdminDeliverableListItemResponse>> listDeliverables(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(deliverableService.listAllForAdmin(page, size));
    }
}
