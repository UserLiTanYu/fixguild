package com.fixguild.account.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志表 sys_login_log。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@Table(value = "sys_login_log")
public class SysLoginLogEntity {

    /**
     * 主键。
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 租户编码。
     */
    private String tenantId;

    /**
     * 用户主键 ID。
     */
    private Long userId;

    /**
     * 用户编码。
     */
    private String userCode;

    /**
     * 登录端编码：platform/tenant/technician。
     */
    private String clientCode;

    /**
     * 登录类型。
     */
    private String loginType;

    /**
     * 登录 IP。
     */
    private String ip;

    /**
     * 浏览器 User-Agent。
     */
    private String userAgent;

    /**
     * 登录状态：1 成功，0 失败。
     */
    private Integer status;

    /**
     * 登录结果说明。
     */
    private String msg;

    /**
     * 登录时间。
     */
    @Column(value = "login_time")
    private LocalDateTime loginTime;
}
