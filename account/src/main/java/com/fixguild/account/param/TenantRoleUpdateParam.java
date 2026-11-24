package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑角色参数。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "编辑角色参数")
public class TenantRoleUpdateParam {

    /** 角色业务 ID */
    @NotBlank(message = "角色 ID 不能为空")
    @Size(max = 32, message = "角色 ID 长度不能超过 32")
    @Schema(description = "角色业务 ID")
    private String uid;

    /** 角色名称 */
    @Size(max = 64, message = "角色名称长度不能超过 64")
    @Schema(description = "角色名称")
    private String roleName;

    /** 数据范围：M2 租户角色只允许 2（本租户） */
    @Min(value = 2, message = "租户角色数据范围只能是 2（本租户）")
    @Max(value = 2, message = "租户角色数据范围只能是 2（本租户）")
    @Schema(description = "数据范围：2 本租户")
    private Integer dataScope;

    /** 状态：1 启用，0 停用 */
    @Min(value = 0, message = "状态值只能是 0 或 1")
    @Max(value = 1, message = "状态值只能是 0 或 1")
    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过 500")
    @Schema(description = "备注")
    private String remark;
}
