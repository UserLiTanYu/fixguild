package com.fixguild.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用是否标记。
 *
 * <p>用于 is_system 等 0/1 语义字段，避免在业务代码中直接写数字。</p>
 *
 * @author litanyu
 * @since 2026-11-27
 */
@Getter
@AllArgsConstructor
public enum YesNo {

    NO(0, "否"),
    YES(1, "是");

    private final Integer code;

    private final String desc;
}
