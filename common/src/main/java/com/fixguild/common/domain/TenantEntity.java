package com.fixguild.common.domain;

import com.mybatisflex.annotation.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带租户字段的实体基类。
 *
 * <p>业务表统一继承本类，MyBatis-Flex 租户插件会自动拼接 tenant_id；
 * 平台级全局表直接继承 {@link BaseEntity}，避免被租户条件过滤。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantEntity extends BaseEntity {

    /** 租户编码，对应 sys_tenant.tenant_code；由租户插件自动拼接查询条件 */
    @Column(tenantId = true)
    private String tenantId;
}
