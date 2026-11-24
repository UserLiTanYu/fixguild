package com.fixguild.account.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色信息出参。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "角色信息")
public class SysRoleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 角色业务 ID */
    @Schema(description = "角色业务 ID")
    private String uid;

    /** 角色编码 */
    @Schema(description = "角色编码")
    private String roleCode;

    /** 角色名称 */
    @Schema(description = "角色名称")
    private String roleName;

    /** 数据范围 */
    @Schema(description = "数据范围：1全部 2本租户 3本人 4本客户")
    private Integer dataScope;

    /** 状态 */
    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    /** 是否系统内置 */
    @Schema(description = "是否系统内置：1是 0否")
    private Integer isSystem;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
