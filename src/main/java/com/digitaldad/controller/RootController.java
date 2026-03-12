package com.digitaldad.controller;

import com.digitaldad.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 根路径控制器
 * <p>提供 API 根路径访问，返回服务运行状态及健康检查接口说明。</p>
 */
@RestController
public class RootController {

    /**
     * 获取 API 根路径欢迎信息
     *
     * @return 包含服务状态说明及健康检查路径的字符串
     */
    @GetMapping("/")
    public Result<String> index() {
        return Result.ok("数字爸爸 API 运行中。健康检查: /api/health");
    }
}
