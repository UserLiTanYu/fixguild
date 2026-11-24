package com.fixguild.account.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联表 sys_user_role。
 *
 * <p>关联表只有创建审计字段，没有 update_by/update_time/remark/is_delete，
 * 因此不继承 BaseEntity；插入时需要显式设置 tenant_id 和创建字段。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Table(value = "sys_user_role")
public class SysUserRoleEntity {

    /** 主键，数据库自增。 */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 租户编码，平台为 0。 */
    @Column(tenantId = true)
    private String tenantId;

    /** 用户 ID。 */
    private Long userId;

    /** 角色 ID。 */
    private Long roleId;

    /** 创建人。 */
    @Column(value = "create_by")
    private String createBy;

    /** 创建时间。 */
    @Column(value = "create_time")
    private LocalDateTime createTime;
}
