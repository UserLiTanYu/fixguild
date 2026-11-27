package com.fixguild.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录端。
 *
 * @author litanyu
 * @since 2026-11-23
 */
@Getter
@AllArgsConstructor
public enum ClientCode {

    PLATFORM("platform", "平台端"),
    TENANT("tenant", "租户端"),
    TECHNICIAN("technician", "技师端");

    private final String code;
    private final String desc;

    /**
     * 根据请求头或路径中的端编码查找枚举，找不到返回 null。
     */
    public static ClientCode fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (ClientCode client : values()) {
            if (client.code.equals(code)) {
                return client;
            }
        }
        return null;
    }
}
