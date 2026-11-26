package com.fixguild.account.entity;

import com.fixguild.common.domain.TenantEntity;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户表 sys_user。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(value = "sys_user")
public class SysUserEntity extends TenantEntity {

    /**
     * 业务 ID。
     */
    private String uid;

    /**
     * 登录端编码：platform/tenant/technician。
     */
    private String clientCode;

    /**
     * 用户编码。
     */
    private String userCode;

    /**
     * 登录名。
     */
    private String username;

    /**
     * 密码（BCrypt 哈希）。
     */
    private String password;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 手机号密文。
     */
    private String mobileCipher;

    /**
     * 手机号哈希，用于等值查询。
     */
    private String mobileHash;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 头像地址。
     */
    private String avatar;

    /**
     * 状态：1 启用，0 停用。
     */
    private Integer status;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录 IP。
     */
    private String lastLoginIp;

    /**
     * 删除序号：0 未删除；删除时置为自身 id，用于释放唯一编码。
     */
    private Long deleteSeq = 0L;
}
