package com.lty.workorder.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 平台端登录参数。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@Schema(description = "平台端登录参数")
public class PlatformLoginParam {

    /** 登录名。 */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    @Schema(description = "登录名", example = "admin")
    private String username;

    /** 密码。 */
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    @Schema(description = "密码", example = "admin123")
    private String password;
}
