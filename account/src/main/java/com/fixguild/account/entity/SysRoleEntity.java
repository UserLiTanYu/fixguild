package com.fixguild.account.entity;

import com.fixguild.common.domain.TenantEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色表 sys_role。
 *
 * <p>角色按租户隔离，平台角色 tenant_id 为 {@code 0}；角色编码在同一租户、
 * 同一登录端内唯一。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_role")
public class SysRoleEntity extends TenantEntity {

    /** 业务 ID。 */
    private String uid;

    /** 登录端：platform/tenant/technician。 */
    private String clientCode;

    /** 角色编码。 */
    private String roleCode;

    /** 角色名称。 */
    private String roleName;

    /** 数据范围：1 全部，2 本租户，3 本人，4 本客户。 */
    private Integer dataScope;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 是否系统内置：1 是，0 否。 */
    private Integer isSystem;

    /** 删除序号：0 未删除；删除时置为自身 id，用于释放唯一编码。 */
    private Long deleteSeq = 0L;
}
