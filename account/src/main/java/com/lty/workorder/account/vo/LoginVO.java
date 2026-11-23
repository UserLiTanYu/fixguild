package com.lty.workorder.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录结果。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录结果")
public class LoginVO {

    /** token 名称。 */
    @Schema(description = "token 名称")
    private String tokenName;

    /** token 值。 */
    @Schema(description = "token 值")
    private String tokenValue;

    /** 用户信息。 */
    @Schema(description = "用户信息")
    private UserInfoVO userInfo;
}
