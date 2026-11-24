package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增角色参数。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "新增角色参数")
public class TenantRoleCreateParam {

    /** 角色编码，租户内唯一，创建后不可修改 */
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64, message = "角色编码长度不能超过 64")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,63}$",
            message = "角色编码必须以字母开头，只能包含字母、数字和下划线")
    @Schema(description = "角色编码", example = "dispatcher")
    private String roleCode;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过 64")
    @Schema(description = "角色名称", example = "调度员")
    private String roleName;

    /** 数据范围：M2 租户角色只允许 2（本租户） */
    @Min(value = 2, message = "租户角色数据范围只能是 2（本租户）")
    @Max(value = 2, message = "租户角色数据范围只能是 2（本租户）")
    @Schema(description = "数据范围：2 本租户", example = "2")
    private Integer dataScope;

    /** 状态：1 启用，0 停用；为空默认启用 */
    @Min(value = 0, message = "状态值只能是 0 或 1")
    @Max(value = 1, message = "状态值只能是 0 或 1")
    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    @Schema(description = "备注")
    private String remark;
}
