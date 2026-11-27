package com.fixguild.common.auth;

/**
 * 登录与鉴权相关常量。
 *
 * @author litanyu
 * @since 2026-11-23
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

    /** 登录会话中的租户编码 key。 */
    public static final String SESSION_TENANT_ID = "tenantId";

    /** 登录会话中的登录端 key。 */
    public static final String SESSION_CLIENT_CODE = "clientCode";

    /** 登录会话中的用户业务 ID key。 */
    public static final String SESSION_USER_UID = "userUid";

    /** 登录会话中的登录名 key。 */
    public static final String SESSION_USERNAME = "username";

    /** 登录会话中的昵称 key。 */
    public static final String SESSION_NICKNAME = "nickname";

    /**
     * Token 剩余有效期小于 30 分钟时续期。
     */
    public static final long RENEW_THRESHOLD_SECONDS = 30 * 60L;

    /**
     * 续期后的有效期：30 天。
     */
    public static final long TOKEN_TIMEOUT_SECONDS = 30 * 24 * 60 * 60L;

    /** MDC 中的租户编码 key。 */
    public static final String MDC_TENANT_ID = "tenantId";

    /** MDC 中的用户编码 key。 */
    public static final String MDC_USER_CODE = "userCode";

    /** MDC 中的登录端 key。 */
    public static final String MDC_CLIENT_CODE = "clientCode";

    private AuthConstants() {
    }
}
