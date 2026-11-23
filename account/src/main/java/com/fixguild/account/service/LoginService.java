package com.fixguild.account.service;

import com.fixguild.account.param.PlatformLoginParam;
import com.fixguild.account.param.TenantLoginParam;
import com.fixguild.account.vo.LoginVO;
import com.fixguild.common.exception.BusinessException;

/**
 * 登录服务。
 *
 * @author litanyu
 * @since 2026-11-19
 */
public interface LoginService {

    /**
     * 平台端登录。
     *
     * @param param 平台端登录参数
     * @return 登录信息
     * @throws BusinessException 用户名或密码错误、账号停用或登录失败次数过多
     */
    LoginVO loginPlatform(PlatformLoginParam param);

    /**
     * 租户端登录。
     *
     * @param param 租户端登录参数
     * @return 登录信息
     * @throws BusinessException 租户不存在、停用、过期，或用户名密码错误、账号停用、登录失败次数过多
     */
    LoginVO loginTenant(TenantLoginParam param);
}
