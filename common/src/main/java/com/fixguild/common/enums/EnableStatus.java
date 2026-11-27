package com.fixguild.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用启停状态。
 *
 * <p>数据库使用 0/1 存储，业务代码统一通过本枚举取值，避免散落魔法数字。</p>
 *
 * @author litanyu
 * @since 2026-11-27
 */
@Getter
@AllArgsConstructor
public enum EnableStatus {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final Integer code;

    private final String desc;
}
