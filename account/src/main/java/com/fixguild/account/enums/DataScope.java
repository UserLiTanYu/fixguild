package com.fixguild.account.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色数据范围。
 *
 * <p>当前 M2 租户角色只允许 {@link #TENANT}；其余取值预留给后续数据权限扩展。</p>
 *
 * @author litanyu
 * @since 2026-11-27
 */
@Getter
@AllArgsConstructor
public enum DataScope {

    ALL(1, "全部"),
    TENANT(2, "本租户"),
    SELF(3, "本人"),
    CUSTOMER(4, "本客户");

    private final Integer code;

    private final String desc;
}
