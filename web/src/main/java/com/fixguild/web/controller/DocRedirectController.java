package com.fixguild.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 兼容原项目的 /doc.html 访问习惯。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Controller
@Tag(name = "公共-接口文档")
public class DocRedirectController {

    /**
     * 兼容旧版 /doc.html 访问路径。
     *
     * @return 重定向到 springdoc Swagger UI
     */
    @GetMapping("/doc.html")
    @Operation(summary = "兼容旧版文档入口")
    public String doc() {
        return "redirect:/swagger-ui/index.html";
    }
}
