package com.fixguild.account.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户用户信息出参。
 *
 * <p>不返回密码、哈希或内部状态字段。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "租户用户信息")
public class TenantUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户业务 ID */
    @Schema(description = "用户业务 ID")
    private String uid;

    /** 用户编码 */
    @Schema(description = "用户编码")
    private String userCode;

    /** 登录名 */
    @Schema(description = "登录名")
    private String username;

    /** 昵称 */
    @Schema(description = "昵称")
    private String nickname;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** 状态 */
    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 角色业务 ID 列表 */
    @Schema(description = "角色业务 ID 列表")
    private List<String> roleUids;

    /** 角色名称列表 */
    @Schema(description = "角色名称列表")
    private List<String> roleNames;
}
