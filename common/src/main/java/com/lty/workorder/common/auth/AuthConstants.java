package com.lty.workorder.common.auth;

/**
 * 登录与鉴权相关常量。
 */
public final class AuthConstants {

    /**
     * 登录 token 请求头，与 Sa-Token 默认 token-name 保持一致。
     */
    public static final String HEADER_TOKEN = "satoken";

    /**
     * 登录端请求头。
     */
    public static final String HEADER_CLIENT_CODE = "client-code";

    /**
     * 平台端租户 ID。
     */
    public static final String PLATFORM_TENANT_ID = "0";

    public static final String SESSION_TENANT_ID = "tenantId";
    public static final String SESSION_CLIENT_CODE = "clientCode";
    public static final String SESSION_USER_UID = "userUid";
    public static final String SESSION_USERNAME = "username";
    public static final String SESSION_NICKNAME = "nickname";

    /**
     * Token 剩余有效期小于 30 分钟时续期。
     */
    public static final long RENEW_THRESHOLD_SECONDS = 30 * 60L;

    /**
     * 续期后的有效期：30 天。
     */
    public static final long TOKEN_TIMEOUT_SECONDS = 30 * 24 * 60 * 60L;

    public static final String MDC_TENANT_ID = "tenantId";
    public static final String MDC_USER_CODE = "userCode";
    public static final String MDC_CLIENT_CODE = "clientCode";

    private AuthConstants() {
    }
}
