package com.fixguild.common.exception;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.fixguild.common.domain.CommonResponse;
import com.fixguild.common.enums.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>所有异常统一转换为 CommonResponse，HTTP 状态码保持 200；客户端只拿到业务码和
 * 安全提示，不返回堆栈、SQL 或内部类名。业务异常、解析失败和唯一键冲突记 warn，
 * 未知异常记 error。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，使用异常携带的业务码。
     *
     * @param e 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResponse<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return CommonResponse.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理未登录或登录态失效。
     *
     * @param e 未登录异常
     * @return 401 或 501 失败响应
     */
    @ExceptionHandler(NotLoginException.class)
    public CommonResponse<Void> handleNotLogin(NotLoginException e) {
        log.debug("未登录: type={}", e.getType());
        ErrorCode errorCode = switch (e.getType()) {
            case NotLoginException.TOKEN_TIMEOUT,
                 NotLoginException.BE_REPLACED,
                 NotLoginException.KICK_OUT,
                 NotLoginException.TOKEN_FREEZE -> ErrorCode.LOGIN_EXPIRED;
            default -> ErrorCode.UNAUTHORIZED;
        };
        return CommonResponse.fail(errorCode);
    }

    /**
     * 处理无权限访问。
     *
     * @param e 无权限异常
     * @return 403 失败响应
     */
    @ExceptionHandler(NotPermissionException.class)
    public CommonResponse<Void> handleNotPermission(NotPermissionException e) {
        log.warn("无权限: permission={}", e.getPermission());
        return CommonResponse.fail(ErrorCode.FORBIDDEN);
    }

    /**
     * 处理无角色访问。
     *
     * @param e 无角色异常
     * @return 403 失败响应
     */
    @ExceptionHandler(NotRoleException.class)
    public CommonResponse<Void> handleNotRole(NotRoleException e) {
        log.warn("无角色: role={}", e.getRole());
        return CommonResponse.fail(ErrorCode.FORBIDDEN);
    }

    /**
     * 处理账号被禁用。
     *
     * @param e 账号禁用异常
     * @return 403 失败响应
     */
    @ExceptionHandler(DisableServiceException.class)
    public CommonResponse<Void> handleDisableService(DisableServiceException e) {
        log.warn("账号被禁用: service={}, level={}", e.getService(), e.getLevel());
        return CommonResponse.fail(ErrorCode.FORBIDDEN, "账号已被禁用");
    }

    /**
     * 处理 {@code @RequestBody} 参数校验失败，拼接字段错误信息。
     *
     * @param e 参数校验异常
     * @return 400 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        // 只拼接字段名和校验消息，不把绑定对象或堆栈返回给客户端
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return CommonResponse.fail(ErrorCode.BAD_REQUEST, msg);
    }

    /**
     * 处理表单/查询参数绑定校验失败。
     *
     * @param e 绑定异常
     * @return 400 失败响应
     */
    @ExceptionHandler(BindException.class)
    public CommonResponse<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return CommonResponse.fail(ErrorCode.BAD_REQUEST, msg);
    }

    /**
     * 处理方法参数上的约束校验失败。
     *
     * @param e 约束校验异常
     * @return 400 失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResponse<Void> handleConstraintViolation(ConstraintViolationException e) {
        return CommonResponse.fail(ErrorCode.BAD_REQUEST, e.getMessage());
    }

    /**
     * 处理请求体 JSON 解析失败。
     *
     * @param e 消息解析异常
     * @return 400 失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public CommonResponse<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败");
        return CommonResponse.fail(ErrorCode.BAD_REQUEST, "请求体格式错误");
    }

    /**
     * 处理请求参数类型不匹配。
     *
     * @param e 类型不匹配异常
     * @return 400 失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public CommonResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return CommonResponse.fail(ErrorCode.BAD_REQUEST, "参数类型错误: " + e.getName());
    }

    /**
     * 处理缺少必填请求参数。
     *
     * @param e 缺少参数异常
     * @return 400 失败响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public CommonResponse<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        return CommonResponse.fail(ErrorCode.BAD_REQUEST, "缺少参数: " + e.getParameterName());
    }

    /**
     * 处理数据库唯一键冲突，兜底并发重复提交。
     *
     * @param e 唯一键冲突异常
     * @return 409 失败响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public CommonResponse<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突");
        return CommonResponse.fail(ErrorCode.CONFLICT, "数据已存在，请勿重复提交");
    }

    /**
     * 处理静态资源或路径不存在。
     *
     * @param e 资源不存在异常
     * @return 404 失败响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public CommonResponse<Void> handleNoResourceFound(NoResourceFoundException e) {
        return CommonResponse.fail(ErrorCode.NOT_FOUND);
    }

    /**
     * 处理未知异常，记录完整堆栈但只返回通用提示。
     *
     * @param e 未知异常
     * @return 500 失败响应
     */
    @ExceptionHandler(Exception.class)
    public CommonResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return CommonResponse.fail(ErrorCode.INTERNAL_ERROR);
    }
}
