package com.fixguild.account.entity;

import com.fixguild.common.domain.BaseEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户表 sys_tenant。
 *
 * <p>租户表本身没有 tenant_id 字段，因此继承 BaseEntity 而不是 TenantEntity；
 * tenant_code 是业务租户号，业务表的 tenant_id 存这个值。联系人手机号 M5 前明文，
 * M5 起改为 SM4 密文。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_tenant")
public class SysTenantEntity extends BaseEntity {

    /** 业务 ID，对外使用；主键 id 不对外暴露 */
    private String uid;

    /** 租户编码，平台内唯一；业务表的 tenant_id 存这个值 */
    private String tenantCode;

    /** 租户名称 */
    private String tenantName;

    /** 联系人 */
    private String contactName;

    /** 联系人手机号；M5 前明文，M5 起 SM4 密文 */
    private String contactMobile;

    /** 状态：1 启用，0 停用 */
    private Integer status;

    /** 到期时间 */
    private LocalDateTime expireTime;

    /** 最大用户数，创建时为空默认 50 */
    private Integer maxUserCount;
}
