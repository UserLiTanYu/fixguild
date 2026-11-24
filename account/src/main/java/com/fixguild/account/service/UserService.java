package com.fixguild.account.service;

import com.fixguild.account.param.PasswordChangeParam;
import com.fixguild.common.auth.LoginUser;
import com.fixguild.common.exception.BusinessException;

/**
 * 用户账号服务。
 *
 * @author litanyu
 * @since 2026-11-24
 */
public interface UserService {

    /**
     * 修改当前登录用户密码。
     *
     * <p>校验旧密码后更新 BCrypt 哈希；调用方在成功后应退出当前登录。</p>
     *
     * @param loginUser 当前登录用户
     * @param param     修改密码参数
     * @throws BusinessException 用户不存在或停用时抛出 401，旧密码错误时抛出 400
     */
    void changePassword(LoginUser loginUser, PasswordChangeParam param);
}
