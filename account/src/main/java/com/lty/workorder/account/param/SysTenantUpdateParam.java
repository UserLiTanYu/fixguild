package com.lty.workorder.account.param;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 编辑租户参数。
 *
 * <p>只更新非空字段；租户编码不可修改，状态变更走独立接口。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编辑租户参数")
public class SysTenantUpdateParam extends SysTenantUidParam {

    /** 租户名称；为 null 表示不修改 */
    @Size(max = 128, message = "租户名称长度不能超过 128")
    @Schema(description = "租户名称")
    private String tenantName;

    /** 联系人；为 null 表示不修改 */
    @Size(max = 64, message = "联系人长度不能超过 64")
    @Schema(description = "联系人")
    private String contactName;

    /** 联系人手机号；为 null 表示不修改 */
    @Size(max = 128, message = "联系人手机号长度不能超过 128")
    @Schema(description = "联系人手机号")
    private String contactMobile;

    /** 到期时间；为 null 表示不修改 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "到期时间")
    private LocalDateTime expireTime;

    /** 最大用户数；为 null 表示不修改 */
    @Min(value = 1, message = "最大用户数必须大于 0")
    @Schema(description = "最大用户数")
    private Integer maxUserCount;

    /** 备注；为 null 表示不修改 */
    @Size(max = 500, message = "备注长度不能超过 500")
    @Schema(description = "备注")
    private String remark;
}
