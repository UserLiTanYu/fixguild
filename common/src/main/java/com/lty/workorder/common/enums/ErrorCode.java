package com.lty.workorder.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码。
 *
 * <p>0 表示成功；4xx/5xx 沿用 HTTP 语义，但通过响应体返回，HTTP 状态码仍为 200。
 * 501 为登录过期，属于业务扩展码，不是标准 HTTP 状态码。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    /** 成功 */
    SUCCESS(0, "success"),
    /** 参数错误 */
    BAD_REQUEST(400, "参数错误"),
    /** 未登录 */
    UNAUTHORIZED(401, "未登录"),
    /** 无权限 */
    FORBIDDEN(403, "无权限"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /** 数据冲突，如唯一键重复 */
    CONFLICT(409, "数据冲突"),
    /** 请求过于频繁 */
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    /** 系统异常 */
    INTERNAL_ERROR(500, "系统异常"),
    /** 登录过期；业务扩展码，不是标准 HTTP 状态码 */
    LOGIN_EXPIRED(501, "登录过期");

    /** 业务码 */
    private final int code;

    /** 默认提示信息 */
    private final String msg;
}
