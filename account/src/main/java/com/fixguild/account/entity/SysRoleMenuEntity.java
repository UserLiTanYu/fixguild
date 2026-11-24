package com.fixguild.account.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色菜单关联表 sys_role_menu。
 *
 * <p>关联表只有创建审计字段，没有 update_by/update_time/remark/is_delete，
 * 因此不继承 BaseEntity；插入时需要显式设置 tenant_id 和创建字段。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Table(value = "sys_role_menu")
public class SysRoleMenuEntity {

    /** 主键，数据库自增。 */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 租户编码，平台为 0。 */
    @Column(tenantId = true)
    private String tenantId;

    /** 角色 ID。 */
    private Long roleId;

    /** 菜单 ID。 */
    private Long menuId;

    /** 创建人。 */
    @Column(value = "create_by")
    private String createBy;

    /** 创建时间。 */
    @Column(value = "create_time")
    private LocalDateTime createTime;
}
