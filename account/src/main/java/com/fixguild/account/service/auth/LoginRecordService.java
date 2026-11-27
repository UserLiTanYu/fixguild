package com.fixguild.account.service.auth;

import com.fixguild.account.dto.LoginClientInfo;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.common.enums.ClientCode;

/**
 * 登录结果记录服务。
 *
 * <p>登录认证本身不开启事务；最后登录时间和登录日志由本服务在独立事务中写入，
 * 避免 Sa-Token 登录态与数据库事务相互影响。写入失败由调用方兜底，
 * 不阻断登录。</p>
 *
 * @author litanyu
 * @since 2026-11-27
 */
public interface LoginRecordService {

    /**
     * 记录登录成功：更新最后登录时间/IP，并写入成功日志。
     *
     * @param user       登录用户
     * @param clientCode 登录端
     * @param clientInfo 客户端信息
     */
    void recordSuccess(SysUserEntity user, ClientCode clientCode, LoginClientInfo clientInfo);

    /**
     * 记录登录失败。
     *
     * @param clientCode 登录端
     * @param tenantId   租户编码
     * @param username   登录名
     * @param user       已匹配到的用户，用户不存在时为 null
     * @param msg        失败原因
     * @param clientInfo 客户端信息
     */
    void recordFailure(ClientCode clientCode, String tenantId, String username,
                       SysUserEntity user, String msg, LoginClientInfo clientInfo);
}
