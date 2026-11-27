package com.fixguild.common.auth;

/**
 * 当前请求登录用户上下文。
 *
 * <p>由鉴权拦截器写入，请求结束后必须清理。</p>
 *
 * @author litanyu
 * @since 2026-11-23
 */
public final class LoginContext {

    private static final ThreadLocal<LoginUser> LOGIN_USER = new ThreadLocal<>();

    private LoginContext() {
    }

    /**
     * 写入当前请求的登录用户。
     *
     * @param loginUser 登录用户
     */
    public static void set(LoginUser loginUser) {
        LOGIN_USER.set(loginUser);
    }

    /**
     * 获取当前请求的登录用户。
     *
     * @return 登录用户，未登录或已清理时返回 null
     */
    public static LoginUser get() {
        return LOGIN_USER.get();
    }

    /**
     * 获取当前登录用户编码。
     *
     * @return 用户编码，未登录时返回 null
     */
    public static String getUserCode() {
        LoginUser loginUser = LOGIN_USER.get();
        return loginUser == null ? null : loginUser.userCode();
    }

    /**
     * 清理当前请求的登录用户，避免线程池复用时串用户。
     */
    public static void clear() {
        LOGIN_USER.remove();
    }
}
