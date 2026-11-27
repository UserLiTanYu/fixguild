package com.fixguild.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 重置租户用户密码结果。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "重置租户用户密码结果")
public class TenantUserResetPasswordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户业务 ID */
    @Schema(description = "用户业务 ID")
    private String uid;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 新密码，仅本次返回 */
    @Schema(description = "新密码，仅本次返回，请妥善保存")
    private String newPassword;
}
