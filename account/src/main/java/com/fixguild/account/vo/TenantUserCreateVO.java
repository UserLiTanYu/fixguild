package com.fixguild.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新增租户用户结果。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "新增租户用户结果")
public class TenantUserCreateVO {

    /** 用户业务 ID */
    @Schema(description = "用户业务 ID")
    private String uid;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 初始密码，仅本次返回 */
    @Schema(description = "初始密码，仅本次返回，请妥善保存")
    private String initialPassword;
}
