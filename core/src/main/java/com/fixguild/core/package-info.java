/**
 * 工单核心模块：工单、派工、客户、设备、技师、附件。
 *
 * <p>依赖 common 和 account，不反向依赖 web；业务表统一继承 TenantEntity，
 * 查询由 MyBatis-Flex 租户插件自动拼接 tenant_id。</p>
 *
 * <p>M3 规划：工单状态机、派单/接单、客户与设备档案。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
package com.fixguild.core;
