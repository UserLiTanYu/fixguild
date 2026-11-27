package com.fixguild.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前用户权限信息。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "当前用户权限信息")
public class UserAuthVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色编码集合。 */
    @Schema(description = "角色编码集合")
    private List<String> roles;

    /** 权限串集合。 */
    @Schema(description = "权限串集合")
    private List<String> permissions;

    /** 菜单树。 */
    @Schema(description = "菜单树")
    private List<MenuTreeVO> menus;
}
