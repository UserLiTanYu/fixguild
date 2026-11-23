package com.fixguild.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档信息与鉴权请求头。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fixguildOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fixguild API")
                        .description("匠盟 · 多租户维保工单 SaaS")
                        .version("v0.1"))
                .components(new Components()
                        .addSecuritySchemes("satoken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("satoken")
                                .description("登录返回的 token"))
                        .addSecuritySchemes("client-code", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("client-code")
                                .description("platform / tenant / technician")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("satoken")
                        .addList("client-code"));
    }
}
