package com.lty.workorder.common.auth;

/**
 * 当前请求登录用户上下文。
 *
 * <p>由鉴权拦截器写入，请求结束后必须清理。</p>
 */
public final class LoginContext {

    private static final ThreadLocal<LoginUser> LOGIN_USER = new ThreadLocal<>();

    private LoginContext() {
    }

    public static void set(LoginUser loginUser) {
        LOGIN_USER.set(loginUser);
    }

    public static LoginUser get() {
        return LOGIN_USER.get();
    }

    public static String getUserCode() {
        LoginUser loginUser = LOGIN_USER.get();
        return loginUser == null ? null : loginUser.userCode();
    }

    public static void clear() {
        LOGIN_USER.remove();
    }
}
