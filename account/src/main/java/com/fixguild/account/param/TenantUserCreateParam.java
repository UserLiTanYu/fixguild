package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增租户用户参数。
 *
 * <p>初始密码由系统生成并只在响应中返回一次，请求不传明文密码。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "新增租户用户参数")
public class TenantUserCreateParam {

    /** 登录名，租户内唯一 */
    @NotBlank(message = "登录名不能为空")
    @Size(max = 64, message = "登录名长度不能超过 64")
    @Schema(description = "登录名", example = "dispatcher01")
    private String username;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64")
    @Schema(description = "昵称", example = "调度员一号")
    private String nickname;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    @Schema(description = "邮箱")
    private String email;

    /** 角色业务 ID 列表，至少一个 */
    @NotEmpty(message = "至少选择一个角色")
    @Schema(description = "角色业务 ID 列表")
    private List<@NotBlank(message = "角色 ID 不能为空") String> roleUids;
}
