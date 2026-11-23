package com.fixguild.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import com.fixguild.common.interceptor.SaTokenAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    /** Sa-Token 鉴权拦截器。 */
    private final SaTokenAuthInterceptor saTokenAuthInterceptor;

    /** {@inheritDoc} */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 先处理登录态和租户上下文
        registry.addInterceptor(saTokenAuthInterceptor)
                .addPathPatterns("/**")
                .order(0);
        // 再处理 @SaCheckPermission / @SaCheckRole 注解
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .order(1);
    }
}
