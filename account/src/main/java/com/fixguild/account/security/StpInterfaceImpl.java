package com.fixguild.account.security;

import cn.dev33.satoken.stp.StpInterface;
import com.fixguild.account.service.auth.AuthService;
import com.fixguild.common.auth.LoginContext;
import com.fixguild.common.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限数据提供者。
 *
 * <p>从当前请求的 LoginContext 取租户和登录端信息，再调用 AuthService 查询角色和权限。
 * 无法确定登录上下文时返回空集合，由 Sa-Token 按无权限处理。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    /** 权限查询服务。 */
    private final AuthService authService;

    /** {@inheritDoc} */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        LoginUser loginUser = currentLogin(loginId);
        return loginUser == null ? List.of() : authService.getPermissionCodes(loginUser);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LoginUser loginUser = currentLogin(loginId);
        return loginUser == null ? List.of() : authService.getRoleCodes(loginUser);
    }

    /**
     * 获取与 Sa-Token loginId 匹配的当前登录用户。
     *
     * @param loginId Sa-Token 登录 ID
     * @return 登录用户，上下文缺失或不匹配时返回 null
     */
    private LoginUser currentLogin(Object loginId) {
        LoginUser loginUser = LoginContext.get();
        if (loginUser == null || !String.valueOf(loginId).equals(loginUser.userCode())) {
            return null;
        }
        return loginUser;
    }
}
