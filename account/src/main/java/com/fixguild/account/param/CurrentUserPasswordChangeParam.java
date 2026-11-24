package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码参数。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "修改密码参数")
public class CurrentUserPasswordChangeParam {

    /** 旧密码。 */
    @NotBlank(message = "旧密码不能为空")
    @Size(max = 64, message = "旧密码长度不能超过 64")
    @Schema(description = "旧密码")
    private String oldPassword;

    /** 新密码。 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度必须在 8 到 64 之间")
    @Schema(description = "新密码，长度 8~64")
    private String newPassword;
}
