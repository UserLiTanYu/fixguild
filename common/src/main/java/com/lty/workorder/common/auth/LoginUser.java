package com.lty.workorder.common.auth;

/**
 * 当前请求的登录用户信息。
 *
 * @param userCode  用户编码，Sa-Token loginId
 * @param userUid   用户业务 ID
 * @param tenantId  租户编码，平台端为 "0"
 * @param clientCode 登录端
 * @param username  登录名
 * @param nickname  昵称
 */
public record LoginUser(
        String userCode,
        String userUid,
        String tenantId,
        String clientCode,
        String username,
        String nickname) {
}
