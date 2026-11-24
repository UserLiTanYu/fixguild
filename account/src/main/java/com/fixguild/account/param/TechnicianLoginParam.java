package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 技师端登录参数。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "技师端登录参数")
public class TechnicianLoginParam {

    /** 租户编码。 */
    @NotBlank(message = "租户编码不能为空")
    @Size(max = 32, message = "租户编码长度不能超过 32")
    @Schema(description = "租户编码", example = "T0001")
    private String tenantCode;

    /** 登录名。 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    @Schema(description = "登录名", example = "technician")
    private String username;

    /** 密码。 */
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    @Schema(description = "密码", example = "admin123")
    private String password;
}
