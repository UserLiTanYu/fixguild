package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑租户用户参数。
 *
 * <p>登录名和用户编码不可修改；roleUids 为空时不调整角色。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "编辑租户用户参数")
public class TenantUserUpdateParam {

    /** 用户业务 ID */
    @NotBlank(message = "用户 ID 不能为空")
    @Size(max = 32, message = "用户 ID 长度不能超过 32")
    @Schema(description = "用户业务 ID")
    private String uid;

    /** 昵称 */
    @Size(max = 64, message = "昵称长度不能超过 64")
    @Schema(description = "昵称")
    private String nickname;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    @Schema(description = "邮箱")
    private String email;

    /** 角色业务 ID 列表；为空表示不调整角色，传入时至少一个 */
    @Size(min = 1, message = "至少选择一个角色")
    @Schema(description = "角色业务 ID 列表")
    private List<@NotBlank(message = "角色 ID 不能为空") String> roleUids;
}
