package com.digitaldad.prompt.controller;

import com.digitaldad.common.result.Result;
import com.digitaldad.prompt.dto.PromptContentDto;
import com.digitaldad.prompt.service.PromptSupplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超管 - 提示词供给预览
 * <p>按场景查看即将生效的提示词列表及合并后的完整内容，用于调试与验收。</p>
 */
@RestController
@RequestMapping("/api/admin/prompts")
@RequiredArgsConstructor
public class AdminPromptSupplyController {

    private final PromptSupplyService supplyService;

    /**
     * 按场景编码获取提示词列表
     *
     * @param sceneCode 场景编码
     * @return 该场景下的提示词内容列表
     */
    @GetMapping("/scene/{sceneCode}")
    public Result<List<PromptContentDto>> getByScene(@PathVariable String sceneCode) {
        return Result.ok(supplyService.getPromptsBySceneCode(sceneCode));
    }

    /**
     * 按场景获取合并后的完整提示词（用于预览实际发给大模型的内容）
     *
     * @param sceneCode 场景编码
     * @return 合并后的提示词字符串
     */
    @GetMapping("/scene/{sceneCode}/combined")
    public Result<String> getCombinedByScene(@PathVariable String sceneCode) {
        return Result.ok(supplyService.getCombinedContentBySceneCode(sceneCode));
    }
}
