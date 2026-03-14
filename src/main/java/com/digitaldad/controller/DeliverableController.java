package com.digitaldad.project.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.project.dto.DeliverableDetailResponse;
import com.digitaldad.project.dto.UpdateDeliverableRequest;
import com.digitaldad.project.service.DeliverableService;
import com.digitaldad.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 统一交付物接口（按 ID 的查/改/删，鉴权：项目归属或超管）
 */
@RestController
@RequestMapping("/api/deliverables")
@RequiredArgsConstructor
public class DeliverableController {

    private final DeliverableService deliverableService;

    @GetMapping("/{id}")
    public Result<DeliverableDetailResponse> getDeliverableById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return Result.ok(deliverableService.getById(id, principal));
    }

    @PutMapping("/{id}")
    public Result<DeliverableDetailResponse> updateDeliverable(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeliverableRequest request) {
        return Result.ok(deliverableService.update(id, principal, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDeliverable(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        deliverableService.delete(id, principal);
        return Result.ok();
    }
}
