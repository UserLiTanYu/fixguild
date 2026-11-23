package com.lty.workorder.web.controller;

import com.lty.workorder.common.domain.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共健康检查接口。
 *
 * <p>无需登录，供探针和负载均衡检查；只返回应用存活状态，不暴露依赖详情。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/api")
@Tag(name = "公共接口")
public class HealthController {

    /**
     * 健康检查。
     *
     * @return 统一响应，data 固定为 UP
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public CommonResponse<String> health() {
        return CommonResponse.ok("UP");
    }
}
