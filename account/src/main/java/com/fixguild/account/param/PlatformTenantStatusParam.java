package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户启停参数。
 *
 * <p>状态值只允许 0/1；不校验状态流转。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户状态参数")
public class PlatformTenantStatusParam extends PlatformTenantUidParam {

    /** 状态：1 启用，0 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值只能是 0 或 1")
    @Max(value = 1, message = "状态值只能是 0 或 1")
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
