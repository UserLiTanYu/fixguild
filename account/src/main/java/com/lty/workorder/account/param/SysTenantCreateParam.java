package com.lty.workorder.account.param;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增租户参数。
 *
 * <p>租户编码平台内唯一；状态为空时默认启用，最大用户数为空时默认 50。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@Schema(description = "新增租户参数")
public class SysTenantCreateParam {

    /** 租户编码，平台内唯一，最长 32 */
    @NotBlank(message = "租户编码不能为空")
    @Size(max = 32, message = "租户编码长度不能超过 32")
    @Schema(description = "租户编码", example = "T0001")
    private String tenantCode;

    /** 租户名称，最长 128 */
    @NotBlank(message = "租户名称不能为空")
    @Size(max = 128, message = "租户名称长度不能超过 128")
    @Schema(description = "租户名称", example = "演示维保公司")
    private String tenantName;

    /** 联系人，最长 64 */
    @Size(max = 64, message = "联系人长度不能超过 64")
    @Schema(description = "联系人")
    private String contactName;

    /** 联系人手机号；M5 前明文存储，M5 起改密文 */
    @Size(max = 128, message = "联系人手机号长度不能超过 128")
    @Schema(description = "联系人手机号")
    private String contactMobile;

    /** 状态：1 启用，0 停用；为空时默认启用 */
    @Min(value = 0, message = "状态值只能是 0 或 1")
    @Max(value = 1, message = "状态值只能是 0 或 1")
    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    /** 到期时间，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "到期时间")
    private LocalDateTime expireTime;

    /** 最大用户数；为空时默认 50 */
    @Min(value = 1, message = "最大用户数必须大于 0")
    @Schema(description = "最大用户数")
    private Integer maxUserCount;

    /** 备注，最长 500 */
    @Size(max = 500, message = "备注长度不能超过 500")
    @Schema(description = "备注")
    private String remark;
}
