package com.fixguild.account.dto;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录请求的客户端信息。
 *
 * <p>由 Controller 从 HTTP 请求提取后传给登录服务，避免 Service 直接依赖
 * HttpServletRequest；User-Agent 可能为空。</p>
 *
 * @param ip        客户端 IP
 * @param userAgent 客户端 User-Agent
 * @author litanyu
 * @since 2026-11-27
 */
public record LoginClientInfo(String ip, String userAgent) {

    /**
     * 从当前 HTTP 请求提取客户端信息。
     *
     * @param request 当前请求，允许为 null
     * @return 客户端信息，request 为空时两个字段均为 null
     */
    public static LoginClientInfo from(HttpServletRequest request) {
        if (request == null) {
            return new LoginClientInfo(null, null);
        }
        return new LoginClientInfo(request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
}
