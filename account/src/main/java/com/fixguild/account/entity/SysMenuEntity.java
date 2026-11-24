package com.fixguild.account.entity;

import com.fixguild.common.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单/权限表 sys_menu。
 *
 * <p>菜单是全局表，不参与租户数据隔离，通过 client_code 区分平台、租户和技师端。
 * 目录和菜单的 perms 可以为空，按钮必须有唯一权限串。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_menu")
public class SysMenuEntity extends BaseEntity {

    /** 业务 ID。 */
    private String uid;

    /** 登录端：platform/tenant/technician。 */
    private String clientCode;

    /** 父菜单 ID，0 为根。 */
    private Long parentId;

    /** 菜单名称。 */
    private String menuName;

    /** 菜单类型：M 目录，C 菜单，F 按钮。 */
    private String menuType;

    /** 路由地址。 */
    private String path;

    /** 前端组件。 */
    private String component;

    /** 权限标识，如 tenant:user:page。 */
    private String perms;

    /** 图标。 */
    private String icon;

    /** 排序。 */
    private Integer sort;

    /** 是否显示：1 是，0 否。 */
    private Integer visible;

    /** 状态：1 启用，0 停用。 */
    private Integer status;

    /** 是否系统内置：1 是，0 否。 */
    private Integer isSystem;
}
