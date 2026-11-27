package com.fixguild.account.service.auth.impl;

import cn.hutool.core.util.StrUtil;
import com.fixguild.account.dto.LoginClientInfo;
import com.fixguild.account.entity.SysLoginLogEntity;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.account.enums.LoginType;
import com.fixguild.account.mapper.SysLoginLogMapper;
import com.fixguild.account.mapper.SysUserMapper;
import com.fixguild.account.service.auth.LoginRecordService;
import com.fixguild.common.enums.ClientCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录结果记录服务实现。
 *
 * <p>成功记录同时更新最后登录时间和成功日志，二者在同一事务内完成；
 * 失败记录只写日志。登录主流程捕获数据库异常，避免审计失败阻断登录。</p>
 *
 * @author litanyu
 * @since 2026-11-27
 */
@Service
@RequiredArgsConstructor
public class LoginRecordServiceImpl implements LoginRecordService {

    /** 登录成功状态。 */
    private static final int LOGIN_SUCCESS = 1;

    /** 登录失败状态。 */
    private static final int LOGIN_FAILURE = 0;

    /** User-Agent 落库最大长度。 */
    private static final int USER_AGENT_MAX_LENGTH = 500;

    /** 用户表 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /** 登录日志 Mapper。 */
    private final SysLoginLogMapper sysLoginLogMapper;

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSuccess(SysUserEntity user, ClientCode clientCode, LoginClientInfo clientInfo) {
        SysUserEntity update = new SysUserEntity();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        update.setLastLoginIp(ip(clientInfo));
        sysUserMapper.update(update, true);

        writeLoginLog(user, clientCode, user.getTenantId(),
                user.getUserCode(), LOGIN_SUCCESS, "登录成功", clientInfo);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFailure(ClientCode clientCode, String tenantId, String username,
                              SysUserEntity user, String msg, LoginClientInfo clientInfo) {
        writeLoginLog(user, clientCode, tenantId,
                user == null ? null : user.getUserCode(), LOGIN_FAILURE,
                msg + ": " + username, clientInfo);
    }

    /**
     * 写入登录日志。
     *
     * @param user       用户实体，可为 null
     * @param clientCode 登录端
     * @param tenantId   租户编码
     * @param userCode   用户编码，可为 null
     * @param status     登录状态
     * @param msg        日志说明
     * @param clientInfo 客户端信息
     */
    private void writeLoginLog(SysUserEntity user, ClientCode clientCode, String tenantId,
                               String userCode, int status, String msg,
                               LoginClientInfo clientInfo) {
        SysLoginLogEntity loginLog = new SysLoginLogEntity();
        loginLog.setTenantId(tenantId);
        loginLog.setUserId(user == null ? null : user.getId());
        loginLog.setUserCode(userCode);
        loginLog.setClientCode(clientCode.getCode());
        loginLog.setLoginType(LoginType.PASSWORD.getCode());
        loginLog.setIp(ip(clientInfo));
        loginLog.setUserAgent(StrUtil.maxLength(userAgent(clientInfo), USER_AGENT_MAX_LENGTH));
        loginLog.setStatus(status);
        loginLog.setMsg(msg);
        loginLog.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(loginLog);
    }

    /**
     * 读取客户端 IP。
     *
     * @param clientInfo 客户端信息
     * @return IP，客户端信息为空时返回 null
     */
    private String ip(LoginClientInfo clientInfo) {
        return clientInfo == null ? null : clientInfo.ip();
    }

    /**
     * 读取客户端 User-Agent。
     *
     * @param clientInfo 客户端信息
     * @return User-Agent，客户端信息为空时返回 null
     */
    private String userAgent(LoginClientInfo clientInfo) {
        return clientInfo == null ? null : clientInfo.userAgent();
    }
}
