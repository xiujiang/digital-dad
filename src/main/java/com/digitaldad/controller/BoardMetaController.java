package com.digitaldad.board.controller;

import com.digitaldad.board.dto.BoardMetaResponse;
import com.digitaldad.board.service.BoardMetaService;
import com.digitaldad.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 主持人 - 板块元数据查询
 * <p>提供已启用的板块列表，供新建项目时选择板块类型。</p>
 */
@RestController
@RequestMapping("/api/b/board-meta")
@RequiredArgsConstructor
public class BoardMetaController {

    private final BoardMetaService boardMetaService;

    /**
     * 获取已启用的板块元数据列表
     *
     * @return 启用状态的板块列表
     */
    @GetMapping("/enabled")
    public Result<List<BoardMetaResponse>> listEnabled() {
        return Result.ok(boardMetaService.listEnabled());
    }
}
