package com.fixguild.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限菜单树节点。
 *
 * <p>按钮（F）是否出现由构建参数决定：权限接口排除按钮，角色分配菜单时包含按钮。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "权限菜单树节点")
public class MenuTreeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 菜单业务 ID。 */
    @Schema(description = "菜单业务 ID")
    private String uid;

    /** 父菜单业务 ID，根节点为空。 */
    @Schema(description = "父菜单业务 ID，根节点为空")
    private String parentUid;

    /** 菜单名称。 */
    @Schema(description = "菜单名称")
    private String menuName;

    /** 菜单类型：M 目录，C 菜单。 */
    @Schema(description = "菜单类型：M 目录，C 菜单")
    private String menuType;

    /** 路由地址。 */
    @Schema(description = "路由地址")
    private String path;

    /** 前端组件。 */
    @Schema(description = "前端组件")
    private String component;

    /** 权限标识。 */
    @Schema(description = "权限标识")
    private String perms;

    /** 图标。 */
    @Schema(description = "图标")
    private String icon;

    /** 排序。 */
    @Schema(description = "排序")
    private Integer sort;

    /** 子菜单。 */
    @Schema(description = "子菜单")
    private List<MenuTreeVO> children = new ArrayList<>();
}
