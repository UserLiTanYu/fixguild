package com.fixguild.account.service.auth.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.fixguild.account.dto.LoginClientInfo;
import com.fixguild.account.entity.SysTenantEntity;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.account.mapper.SysTenantMapper;
import com.fixguild.account.mapper.SysUserMapper;
import com.fixguild.account.param.PlatformLoginParam;
import com.fixguild.account.param.TechnicianLoginParam;
import com.fixguild.account.param.TenantLoginParam;
import com.fixguild.account.security.LoginAttemptGuard;
import com.fixguild.account.service.auth.LoginRecordService;
import com.fixguild.account.service.auth.LoginService;
import com.fixguild.account.support.AccountCredentialGenerator;
import com.fixguild.account.vo.LoginVO;
import com.fixguild.account.vo.UserInfoVO;
import com.fixguild.common.auth.AuthConstants;
import com.fixguild.common.enums.ClientCode;
import com.fixguild.common.enums.EnableStatus;
import com.fixguild.common.enums.ErrorCode;
import com.fixguild.common.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录服务实现。
 *
 * <p>认证流程不开启数据库事务；登录成功/失败记录由 {@link LoginRecordService}
 * 在独立事务中写入。记录失败只告警，不阻断登录，避免审计副作用影响登录可用性。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    /** 用户 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /** 租户 Mapper。 */
    private final SysTenantMapper sysTenantMapper;

    /** 登录失败锁定器。 */
    private final LoginAttemptGuard loginAttemptGuard;

    /** 登录结果记录服务。 */
    private final LoginRecordService loginRecordService;

    /** {@inheritDoc} */
    @Override
    public LoginVO loginPlatform(PlatformLoginParam param, LoginClientInfo clientInfo) {
        return authenticate(ClientCode.PLATFORM, AuthConstants.PLATFORM_TENANT_ID,
                param.getUsername(), param.getPassword(), clientInfo);
    }

    /** {@inheritDoc} */
    @Override
    public LoginVO loginTenant(TenantLoginParam param, LoginClientInfo clientInfo) {
        SysTenantEntity tenant = requireActiveTenant(param.getTenantCode());
        return authenticate(ClientCode.TENANT, tenant.getTenantCode(),
                param.getUsername(), param.getPassword(), clientInfo);
    }

    /** {@inheritDoc} */
    @Override
    public LoginVO loginTechnician(TechnicianLoginParam param, LoginClientInfo clientInfo) {
        SysTenantEntity tenant = requireActiveTenant(param.getTenantCode());
        return authenticate(ClientCode.TECHNICIAN, tenant.getTenantCode(),
                param.getUsername(), param.getPassword(), clientInfo);
    }

    /**
     * 校验租户存在、启用且未过期。
     *
     * @param tenantCode 租户编码
     * @return 租户实体
     */
    private SysTenantEntity requireActiveTenant(String tenantCode) {
        SysTenantEntity tenant = sysTenantMapper.selectOneByQuery(QueryWrapper.create()
                .eq(SysTenantEntity::getTenantCode, tenantCode));
        if (tenant == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "租户不存在");
        }
        if (!EnableStatus.ENABLED.getCode().equals(tenant.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "租户已停用");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "租户已过期");
        }
        return tenant;
    }

    /**
     * 执行登录校验、锁定检查和登录态写入。
     *
     * @param clientCode 登录端
     * @param tenantId   租户编码
     * @param username   登录名
     * @param password   原始密码
     * @param clientInfo 客户端信息
     * @return 登录信息
     */
    private LoginVO authenticate(ClientCode clientCode, String tenantId, String username,
                                 String password, LoginClientInfo clientInfo) {
        if (loginAttemptGuard.isLocked(clientCode, tenantId, username)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录失败次数过多，请 15 分钟后再试");
        }

        SysUserEntity user = sysUserMapper.selectOneByQuery(QueryWrapper.create()
                .eq(SysUserEntity::getTenantId, tenantId)
                .eq(SysUserEntity::getClientCode, clientCode.getCode())
                .eq(SysUserEntity::getUsername, username));

        if (user == null || !AccountCredentialGenerator.matchesPassword(password, user.getPassword())) {
            loginAttemptGuard.recordFailure(clientCode, tenantId, username);
            recordFailureSafely(clientCode, tenantId, username, user, "用户名或密码错误", clientInfo);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!EnableStatus.ENABLED.getCode().equals(user.getStatus())) {
            recordFailureSafely(clientCode, tenantId, username, user, "账号已停用", clientInfo);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已停用");
        }

        loginAttemptGuard.clear(clientCode, tenantId, username);

        StpUtil.login(user.getUserCode(), new SaLoginParameter().setDeviceType(clientCode.getCode()));
        SaSession tokenSession = StpUtil.getTokenSession();
        tokenSession.set(AuthConstants.SESSION_TENANT_ID, user.getTenantId());
        tokenSession.set(AuthConstants.SESSION_CLIENT_CODE, clientCode.getCode());
        tokenSession.set(AuthConstants.SESSION_USER_UID, user.getUid());
        tokenSession.set(AuthConstants.SESSION_USERNAME, user.getUsername());
        tokenSession.set(AuthConstants.SESSION_NICKNAME, user.getNickname());

        recordSuccessSafely(user, clientCode, clientInfo);
        log.info("登录成功: client={}, tenantId={}, userCode={}",
                clientCode.getCode(), tenantId, user.getUserCode());

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return new LoginVO(tokenInfo.getTokenName(), tokenInfo.getTokenValue(), toUserInfo(user));
    }

    /**
     * 记录登录成功，数据库异常只告警，不影响登录结果。
     *
     * @param user       登录用户
     * @param clientCode 登录端
     * @param clientInfo 客户端信息
     */
    private void recordSuccessSafely(SysUserEntity user, ClientCode clientCode, LoginClientInfo clientInfo) {
        try {
            loginRecordService.recordSuccess(user, clientCode, clientInfo);
        } catch (DataAccessException e) {
            log.warn("写入登录成功记录失败: client={}, userCode={}",
                    clientCode.getCode(), user.getUserCode(), e);
        }
    }

    /**
     * 记录登录失败，数据库异常只告警，不影响失败结果。
     *
     * @param clientCode 登录端
     * @param tenantId   租户编码
     * @param username   登录名
     * @param user       已匹配用户，可为 null
     * @param msg        失败原因
     * @param clientInfo 客户端信息
     */
    private void recordFailureSafely(ClientCode clientCode, String tenantId, String username,
                                     SysUserEntity user, String msg, LoginClientInfo clientInfo) {
        try {
            loginRecordService.recordFailure(clientCode, tenantId, username, user, msg, clientInfo);
        } catch (DataAccessException e) {
            log.warn("写入登录失败记录失败: client={}, username={}", clientCode.getCode(), username, e);
        }
    }

    /**
     * 将用户实体转换为用户信息 VO。
     *
     * @param user 用户实体
     * @return 用户信息
     */
    private UserInfoVO toUserInfo(SysUserEntity user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUid(user.getUid());
        vo.setUserCode(user.getUserCode());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setTenantId(user.getTenantId());
        vo.setClientCode(user.getClientCode());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        return vo;
    }
}
