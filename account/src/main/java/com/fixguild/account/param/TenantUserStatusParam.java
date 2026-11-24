package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 租户用户启停参数。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "租户用户启停参数")
public class TenantUserStatusParam {

    /** 用户业务 ID */
    @NotBlank(message = "用户 ID 不能为空")
    @Size(max = 32, message = "用户 ID 长度不能超过 32")
    @Schema(description = "用户业务 ID")
    private String uid;

    /** 状态：1 启用，0 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值只能是 0 或 1")
    @Max(value = 1, message = "状态值只能是 0 或 1")
    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
