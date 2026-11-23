package com.lty.workorder.account.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户信息出参。
 *
 * <p>联系人手机号已脱敏，不返回密文或明文；时间字段统一按 GMT+8 输出。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@Schema(description = "租户信息")
public class SysTenantVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 租户业务 ID */
    @Schema(description = "租户业务ID")
    private String uid;

    /** 租户编码，业务表 tenant_id 存这个值 */
    @Schema(description = "租户编码")
    private String tenantCode;

    /** 租户名称 */
    @Schema(description = "租户名称")
    private String tenantName;

    /** 联系人 */
    @Schema(description = "联系人")
    private String contactName;

    /** 联系人手机号，已脱敏 */
    @Schema(description = "联系人手机号（脱敏）")
    private String contactMobile;

    /** 状态：1 启用，0 停用 */
    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    /** 到期时间，格式 yyyy-MM-dd HH:mm:ss */
    @Schema(description = "到期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;

    /** 最大用户数 */
    @Schema(description = "最大用户数")
    private Integer maxUserCount;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建时间，格式 yyyy-MM-dd HH:mm:ss */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /** 更新时间，格式 yyyy-MM-dd HH:mm:ss */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
