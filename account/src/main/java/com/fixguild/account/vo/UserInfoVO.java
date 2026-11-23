package com.fixguild.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前登录用户信息。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfoVO {

    /** 用户业务 ID。 */
    @Schema(description = "用户业务 ID")
    private String uid;

    /** 用户编码。 */
    @Schema(description = "用户编码")
    private String userCode;

    /** 登录名。 */
    @Schema(description = "登录名")
    private String username;

    /** 昵称。 */
    @Schema(description = "昵称")
    private String nickname;

    /** 租户编码。 */
    @Schema(description = "租户编码")
    private String tenantId;

    /** 登录端。 */
    @Schema(description = "登录端")
    private String clientCode;

    /** 头像。 */
    @Schema(description = "头像")
    private String avatar;

    /** 邮箱。 */
    @Schema(description = "邮箱")
    private String email;
}
