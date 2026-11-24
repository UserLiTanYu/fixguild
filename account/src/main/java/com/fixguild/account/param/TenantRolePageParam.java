package com.fixguild.account.param;

import com.fixguild.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色分页查询参数。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色分页参数")
public class TenantRolePageParam extends PageParam {

    /** 角色编码，精确匹配；为空不过滤 */
    @Size(max = 64, message = "角色编码长度不能超过 64")
    @Schema(description = "角色编码")
    private String roleCode;

    /** 角色名称，模糊匹配；为空不过滤 */
    @Size(max = 64, message = "角色名称长度不能超过 64")
    @Schema(description = "角色名称")
    private String roleName;

    /** 状态：1 启用，0 停用；为空不过滤 */
    @Min(value = 0, message = "状态值只能是 0 或 1")
    @Max(value = 1, message = "状态值只能是 0 或 1")
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
