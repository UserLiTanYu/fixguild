package com.fixguild.web;

import com.fixguild.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @IgnoreTenant} 跨租户查询测试。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class IgnoreTenantTest {

    /** 测试用租户查询服务。 */
    @Autowired
    private TestTenantQueryService testTenantQueryService;

    /** 每个测试后清理租户上下文。 */
    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    /** @IgnoreTenant 方法应忽略租户过滤。 */
    @Test
    void annotatedMethodShouldIgnoreTenantFilter() {
        TenantContext.clear();
        long allUsers = testTenantQueryService.countCurrentTenantUsers();

        TenantContext.set("T0001");
        long tenantUsers = testTenantQueryService.countCurrentTenantUsers();

        assertThat(tenantUsers).isPositive();
        assertThat(tenantUsers).isLessThan(allUsers);
        assertThat(testTenantQueryService.countAllUsers()).isEqualTo(allUsers);
    }
}
