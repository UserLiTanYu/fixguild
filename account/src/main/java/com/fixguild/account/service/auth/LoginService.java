package com.fixguild.account.service.auth;

import com.fixguild.account.dto.LoginClientInfo;
import com.fixguild.account.param.PlatformLoginParam;
import com.fixguild.account.param.TechnicianLoginParam;
import com.fixguild.account.param.TenantLoginParam;
import com.fixguild.account.vo.LoginVO;
import com.fixguild.common.exception.BusinessException;

/**
 * 登录服务。
 *
 * <p>认证流程不开启数据库事务；最后登录时间和登录日志由
 * {@link LoginRecordService} 独立事务记录，避免与 Sa-Token 登录态耦合。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
public interface LoginService {

    /**
     * 平台端登录。
     *
     * @param param      平台端登录参数
     * @param clientInfo 客户端 IP/User-Agent
     * @return 登录信息
     * @throws BusinessException 用户名或密码错误、账号停用或登录失败次数过多
     */
    LoginVO loginPlatform(PlatformLoginParam param, LoginClientInfo clientInfo);

    /**
     * 租户端登录。
     *
     * @param param      租户端登录参数
     * @param clientInfo 客户端 IP/User-Agent
     * @return 登录信息
     * @throws BusinessException 租户不存在、停用、过期，或用户名密码错误、账号停用、登录失败次数过多
     */
    LoginVO loginTenant(TenantLoginParam param, LoginClientInfo clientInfo);

    /**
     * 技师端登录。
     *
     * @param param      技师端登录参数
     * @param clientInfo 客户端 IP/User-Agent
     * @return 登录信息
     * @throws BusinessException 租户不存在、停用、过期，或用户名密码错误、账号停用、登录失败次数过多
     */
    LoginVO loginTechnician(TechnicianLoginParam param, LoginClientInfo clientInfo);
}
