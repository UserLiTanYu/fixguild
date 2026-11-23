package com.lty.workorder.common.exception;

import com.lty.workorder.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>继承 RuntimeException，由 GlobalExceptionHandler 统一转换为 CommonResponse；
 * code 使用 ErrorCode 的业务码，不直接映射 HTTP 状态码。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务码 */
    private final int code;

    /**
     * 使用错误码的默认消息构造异常。
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码和自定义消息构造异常。
     *
     * @param errorCode 错误码
     * @param message   自定义提示信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用自定义业务码和消息构造异常。
     *
     * @param code    业务码
     * @param message 提示信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
