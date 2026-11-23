package com.lty.workorder.account.param;

import com.lty.workorder.common.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户分页查询参数。
 *
 * <p>所有筛选条件为空时不过滤；分页参数由 PageParam 统一限制。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户分页参数")
public class SysTenantPageParam extends PageParam {

    /** 租户编码，精确匹配；为空不过滤 */
    @Size(max = 32, message = "租户编码长度不能超过 32")
    @Schema(description = "租户编码")
    private String tenantCode;

    /** 租户名称，模糊匹配；为空不过滤 */
    @Size(max = 128, message = "租户名称长度不能超过 128")
    @Schema(description = "租户名称")
    private String tenantName;

    /** 状态：1 启用，0 停用；为空不过滤 */
    @Min(value = 0, message = "状态值只能是 0 或 1")
    @Max(value = 1, message = "状态值只能是 0 或 1")
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
