package com.lty.workorder.common.auth;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 鉴权白名单。
 *
 * <p>白名单集中维护，禁止在拦截器或 Controller 中散落判断。</p>
 */
@Component
public class AuthWhitelist {

    private static final List<String> PATTERNS = List.of(
            "/api/health",
            "/actuator/health",
            "/actuator/info",
            "/error",
            "/favicon.ico",
            // 登录与短信
            "/account/api/*/login",
            "/account/api/*/sms/send",
            // 本地 H2 控制台
            "/h2-console",
            "/h2-console/**",
            // 接口文档
            "/doc.html",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean matches(String uri) {
        return PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
}
