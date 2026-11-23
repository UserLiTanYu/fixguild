package com.lty.workorder.common.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.lty.workorder.common.auth.AuthConstants;
import com.lty.workorder.common.auth.AuthWhitelist;
import com.lty.workorder.common.auth.LoginContext;
import com.lty.workorder.common.auth.LoginUser;
import com.lty.workorder.common.enums.ClientCode;
import com.lty.workorder.common.enums.ErrorCode;
import com.lty.workorder.common.exception.BusinessException;
import com.lty.workorder.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sa-Token 鉴权拦截器。
 *
 * <p>负责校验登录态、校验登录端、写入租户/登录用户上下文、续期，并在请求结束后清理线程变量。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SaTokenAuthInterceptor implements HandlerInterceptor {

    private final AuthWhitelist authWhitelist;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 防御式清理，避免容器线程复用导致上下文串数据
        TenantContext.clear();
        LoginContext.clear();



        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        if (authWhitelist.matches(request.getRequestURI())) {
            return true;
        }

        StpUtil.checkLogin();

        String clientCodeHeader = request.getHeader(AuthConstants.HEADER_CLIENT_CODE);
        ClientCode clientCode = ClientCode.fromCode(clientCodeHeader);
        if (clientCode == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "client-code 不合法");
        }
        if (!clientCode.getCode().equals(StpUtil.getLoginDeviceType())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录端不匹配");
        }

        SaSession tokenSession = StpUtil.getTokenSession();
        String tenantId = asString(tokenSession.get(AuthConstants.SESSION_TENANT_ID));
        if (StrUtil.isBlank(tenantId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录会话缺少租户信息");
        }

        String userCode = String.valueOf(StpUtil.getLoginId());
        String userUid = asString(tokenSession.get(AuthConstants.SESSION_USER_UID));
        String username = asString(tokenSession.get(AuthConstants.SESSION_USERNAME));
        String nickname = asString(tokenSession.get(AuthConstants.SESSION_NICKNAME));

        TenantContext.set(tenantId);
        LoginContext.set(new LoginUser(userCode, userUid, tenantId, clientCode.getCode(), username, nickname));

        MDC.put(AuthConstants.MDC_TENANT_ID, tenantId);
        MDC.put(AuthConstants.MDC_USER_CODE, userCode);
        MDC.put(AuthConstants.MDC_CLIENT_CODE, clientCode.getCode());

        if (StpUtil.getTokenTimeout() < AuthConstants.RENEW_THRESHOLD_SECONDS) {
            StpUtil.renewTimeout(AuthConstants.TOKEN_TIMEOUT_SECONDS);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
        LoginContext.clear();
        MDC.remove(AuthConstants.MDC_TENANT_ID);
        MDC.remove(AuthConstants.MDC_USER_CODE);
        MDC.remove(AuthConstants.MDC_CLIENT_CODE);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
