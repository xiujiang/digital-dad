package com.digitaldad.controller;

import com.digitaldad.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * <p>提供服务的存活探针接口，用于负载均衡、K8s 探针等场景。</p>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 健康检查
     *
     * @return 返回 "ok" 表示服务正常
     */
    @GetMapping
    public Result<String> health() {
        return Result.ok("ok");
    }
}
