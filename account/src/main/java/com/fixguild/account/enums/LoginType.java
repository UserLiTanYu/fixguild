package com.fixguild.account.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录方式。
 *
 * <p>与 sys_login_log.login_type 对应；当前只启用密码登录。</p>
 *
 * @author litanyu
 * @since 2026-11-27
 */
@Getter
@AllArgsConstructor
public enum LoginType {

    PASSWORD("PASSWORD", "密码登录"),
    SMS("SMS", "短信登录");

    private final String code;

    private final String desc;
}
