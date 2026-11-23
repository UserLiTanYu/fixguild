package com.lty.workorder.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lty.workorder.common.enums.ErrorCode;
import com.lty.workorder.common.filter.TraceIdFilter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 统一 API 响应。
 *
 * <p>HTTP 状态码固定为 200，业务结果由 code 表达；traceId 从 MDC 读取，
 * 便于日志与响应串联。成功时 code 为 0。</p>
 *
 * @param <T> 业务数据类型
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务码，0 表示成功 */
    private Integer code;

    /** 提示信息，成功时为 success */
    private String msg;

    /** 业务数据，失败时为 null */
    private T data;

    /** 链路追踪 ID，来自 MDC */
    private String traceId;

    /**
     * 构造成功响应。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功响应
     */
    public static <T> CommonResponse<T> ok(T data) {
        return new CommonResponse<>(ErrorCode.SUCCESS.getCode(), "success", data, currentTraceId());
    }

    /**
     * 构造无数据的成功响应。
     *
     * @return 成功响应
     */
    public static CommonResponse<Void> ok() {
        return new CommonResponse<>(ErrorCode.SUCCESS.getCode(), "success", null, currentTraceId());
    }

    /**
     * 构造失败响应。
     *
     * @param code 业务码
     * @param msg  提示信息
     * @param <T>  业务数据类型
     * @return 失败响应
     */
    public static <T> CommonResponse<T> fail(int code, String msg) {
        return new CommonResponse<>(code, msg, null, currentTraceId());
    }

    /**
     * 使用统一错误码构造失败响应。
     *
     * @param errorCode 错误码
     * @param <T>       业务数据类型
     * @return 失败响应
     */
    public static <T> CommonResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMsg());
    }

    /**
     * 使用统一错误码和自定义消息构造失败响应。
     *
     * @param errorCode 错误码
     * @param msg       自定义提示信息
     * @param <T>       业务数据类型
     * @return 失败响应
     */
    public static <T> CommonResponse<T> fail(ErrorCode errorCode, String msg) {
        return fail(errorCode.getCode(), msg);
    }

    /**
     * 判断当前响应是否成功。
     *
     * <p>仅用于内部判断，序列化时通过 {@code @JsonIgnore} 忽略。</p>
     *
     * @return 成功返回 true
     */
    @JsonIgnore
    public boolean isSuccess() {
        return Objects.equals(ErrorCode.SUCCESS.getCode(), code);
    }

    /**
     * 从 MDC 读取当前请求的 traceId。
     *
     * @return traceId；非请求线程中可能为 null
     */
    private static String currentTraceId() {
        return MDC.get(TraceIdFilter.TRACE_ID);
    }
}
