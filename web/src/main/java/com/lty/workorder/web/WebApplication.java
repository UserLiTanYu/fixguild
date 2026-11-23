package com.lty.workorder.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 应用启动入口。
 *
 * <p>统一扫描 com.lty.workorder；Mapper 分布在 account/core 两个模块。
 * 默认激活 local profile（H2），连接 MySQL 时显式指定 dev。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.lty.workorder")
@MapperScan(basePackages = {
        "com.lty.workorder.account.mapper",
        "com.lty.workorder.core.mapper"
})
public class WebApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }
}
