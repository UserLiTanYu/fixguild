package com.fixguild.common.tenant;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.tenant.TenantFactory;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Flex 多租户工厂。
 *
 * <p>租户来源必须是 TenantContext，禁止在实现里硬编码租户 ID；返回 null 表示
 * 不拼接租户条件，平台端全局表、显式忽略租户或没有租户上下文的场景会返回 null。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Component
public class MyTenantFactory implements TenantFactory {

    /**
     * 返回当前线程的租户 ID。
     *
     * @param tableName 当前操作的表名，可用于按表定制租户规则
     * @return 租户 ID 数组；返回 null 表示不拼接租户条件
     */
    @Override
    public Object[] getTenantIds(String tableName) {
        if (TenantContext.isIgnore()) {
            return null;
        }
        String tenantId = TenantContext.get();
        // 未登录或平台初始化阶段没有租户上下文，此时不追加租户条件
        return StrUtil.isBlank(tenantId) ? null : new Object[]{tenantId};
    }

    /**
     * 兼容旧接口，新版本会调用 {@link #getTenantIds(String)}。
     *
     * @return 租户 ID 数组
     * @deprecated 使用 {@link #getTenantIds(String)} 代替
     */
    @Override
    @Deprecated
    public Object[] getTenantIds() {
        return getTenantIds(null);
    }
}
