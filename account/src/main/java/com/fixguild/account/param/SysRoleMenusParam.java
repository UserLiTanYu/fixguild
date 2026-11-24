package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色菜单分配参数。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "角色菜单分配参数")
public class SysRoleMenusParam {

    /** 角色业务 ID */
    @NotBlank(message = "角色 ID 不能为空")
    @Size(max = 32, message = "角色 ID 长度不能超过 32")
    @Schema(description = "角色业务 ID")
    private String uid;

    /** 菜单业务 ID 列表，空列表表示清空角色菜单 */
    @NotNull(message = "菜单列表不能为空")
    @Size(max = 500, message = "菜单数量不能超过 500")
    @Schema(description = "菜单业务 ID 列表")
    private List<@NotBlank(message = "菜单 ID 不能为空") String> menuUids;
}
