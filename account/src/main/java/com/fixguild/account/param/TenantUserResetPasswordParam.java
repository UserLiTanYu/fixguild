package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置租户用户密码参数。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "重置租户用户密码参数")
public class TenantUserResetPasswordParam {

    /** 用户业务 ID */
    @NotBlank(message = "用户 ID 不能为空")
    @Size(max = 32, message = "用户 ID 长度不能超过 32")
    @Schema(description = "用户业务 ID")
    private String uid;
}
