package com.fixguild.account.service.auth.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.fixguild.account.entity.SysLoginLogEntity;
import com.fixguild.account.entity.SysTenantEntity;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.account.mapper.SysLoginLogMapper;
import com.fixguild.account.mapper.SysTenantMapper;
import com.fixguild.account.mapper.SysUserMapper;
import com.fixguild.account.param.PlatformLoginParam;
import com.fixguild.account.param.TechnicianLoginParam;
import com.fixguild.account.param.TenantLoginParam;
import com.fixguild.account.security.LoginAttemptGuard;
import com.fixguild.account.service.auth.LoginService;
import com.fixguild.account.vo.LoginVO;
import com.fixguild.account.vo.UserInfoVO;
import com.fixguild.common.auth.AuthConstants;
import com.fixguild.common.enums.ClientCode;
import com.fixguild.common.enums.ErrorCode;
import com.fixguild.common.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 登录服务实现。
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

    /** 登录日志 Mapper。 */
    private final SysLoginLogMapper sysLoginLogMapper;

    /** 登录失败锁定器。 */
    private final LoginAttemptGuard loginAttemptGuard;

    /** {@inheritDoc} */
    @Override
    public LoginVO loginPlatform(PlatformLoginParam param) {
        return doLogin(ClientCode.PLATFORM, AuthConstants.PLATFORM_TENANT_ID,
                param.getUsername(), param.getPassword());
    }

    /** {@inheritDoc} */
    @Override
    public LoginVO loginTenant(TenantLoginParam param) {
        SysTenantEntity tenant = requireActiveTenant(param.getTenantCode());
        return doLogin(ClientCode.TENANT, tenant.getTenantCode(),
                param.getUsername(), param.getPassword());
    }

    /** {@inheritDoc} */
    @Override
    public LoginVO loginTechnician(TechnicianLoginParam param) {
        SysTenantEntity tenant = requireActiveTenant(param.getTenantCode());
        return doLogin(ClientCode.TECHNICIAN, tenant.getTenantCode(),
                param.getUsername(), param.getPassword());
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
        if (!Integer.valueOf(1).equals(tenant.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "租户已停用");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "租户已过期");
        }
        return tenant;
    }

    /** 执行登录校验、锁定检查和登录态写入。 */
    private LoginVO doLogin(ClientCode clientCode, String tenantId, String username, String password) {

        //检查账号是否被锁定
        if (loginAttemptGuard.isLocked(clientCode, tenantId, username)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录失败次数过多，请 15 分钟后再试");
        }

        //根据条件查找用户信息
        SysUserEntity user = sysUserMapper.selectOneByQuery(QueryWrapper.create()
                .eq(SysUserEntity::getTenantId, tenantId)
                .eq(SysUserEntity::getClientCode, clientCode.getCode())
                .eq(SysUserEntity::getUsername, username));

        //用户不存在或者密码错误
        if (user == null || !passwordMatches(password, user.getPassword())) {

            //失败次数+1
            loginAttemptGuard.recordFailure(clientCode, tenantId, username);
            //写入登陆失败日志
            writeLoginLog(user, clientCode, tenantId, username, false, "用户名或密码错误");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        //账号被停用
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            //写入登陆失败日志
            writeLoginLog(user, clientCode, tenantId, username, false, "账号已停用");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号已停用");
        }

        //清除登陆失败记录
        loginAttemptGuard.clear(clientCode, tenantId, username);

        //获取登录ip
        HttpServletRequest request = currentRequest();
        String ip = request == null ? null : request.getRemoteAddr();

        StpUtil.login(user.getUserCode(), new SaLoginParameter().setDeviceType(clientCode.getCode()));
        //获取tokenSession
        SaSession tokenSession = StpUtil.getTokenSession();
        //向tokenSession里面存入相关的值
        tokenSession.set(AuthConstants.SESSION_TENANT_ID, user.getTenantId());
        tokenSession.set(AuthConstants.SESSION_CLIENT_CODE, clientCode.getCode());
        tokenSession.set(AuthConstants.SESSION_USER_UID, user.getUid());
        tokenSession.set(AuthConstants.SESSION_USERNAME, user.getUsername());
        tokenSession.set(AuthConstants.SESSION_NICKNAME, user.getNickname());

        //修改最后登录时间和ip
        SysUserEntity update = new SysUserEntity();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        update.setLastLoginIp(ip);
        sysUserMapper.update(update, true);

        //写入登陆成功日志
        writeLoginLog(user, clientCode, tenantId, username, true, "登录成功");
        log.info("登录成功: client={}, tenantId={}, userCode={}", clientCode.getCode(), tenantId, user.getUserCode());

        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        return new LoginVO(tokenInfo.getTokenName(), tokenInfo.getTokenValue(), toUserInfo(user));
    }

    /** 校验原始密码与密文是否匹配。 */
    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        if (StrUtil.isBlank(encodedPassword)) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (IllegalArgumentException e) {
            log.warn("密码哈希格式不正确");
            return false;
        }
    }

    /** 写入登录日志，失败不影响登录主流程。 */
    private void writeLoginLog(SysUserEntity user, ClientCode clientCode, String tenantId,
                               String username, boolean success, String msg) {
        try {
            HttpServletRequest request = currentRequest();
            SysLoginLogEntity loginLog = new SysLoginLogEntity();
            loginLog.setTenantId(tenantId);
            loginLog.setUserId(user == null ? null : user.getId());
            loginLog.setUserCode(user == null ? null : user.getUserCode());
            loginLog.setClientCode(clientCode.getCode());
            loginLog.setLoginType("PASSWORD");
            loginLog.setIp(request == null ? null : request.getRemoteAddr());
            loginLog.setUserAgent(request == null ? null : StrUtil.maxLength(request.getHeader("User-Agent"), 500));
            loginLog.setStatus(success ? 1 : 0);
            loginLog.setMsg(success ? msg : msg + ": " + username);
            loginLog.setLoginTime(LocalDateTime.now());
            sysLoginLogMapper.insert(loginLog);
        } catch (Exception e) {
            // 登录日志失败不影响登录主流程
            log.warn("写入登录日志失败: client={}, username={}", clientCode.getCode(), username, e);
        }
    }

    /** 获取当前 HTTP 请求。 */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /** 将用户实体转换为用户信息 VO。 */
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
