package com.fixguild.account.service;

import com.fixguild.account.param.TenantUserCreateParam;
import com.fixguild.account.param.TenantUserPageParam;
import com.fixguild.account.param.TenantUserResetPasswordParam;
import com.fixguild.account.param.TenantUserStatusParam;
import com.fixguild.account.param.TenantUserUpdateParam;
import com.fixguild.account.vo.TenantUserCreateVO;
import com.fixguild.account.vo.TenantUserResetPasswordVO;
import com.fixguild.account.vo.TenantUserVO;
import com.fixguild.common.domain.PageResult;
import com.fixguild.common.exception.BusinessException;

/**
 * 租户端用户管理服务。
 *
 * <p>只管理 client_code=tenant 的用户；平台用户和技师用户不在本服务范围内。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
public interface TenantUserService {

    /**
     * 分页查询当前租户用户。
     *
     * @param param 分页与筛选参数
     * @return 用户分页结果
     */
    PageResult<TenantUserVO> page(TenantUserPageParam param);

    /**
     * 新增租户用户并绑定角色。
     *
     * @param param 新增参数
     * @return 用户 ID、登录名和一次性初始密码
     * @throws BusinessException 用户名重复、超过用户上限或角色无效时抛出 400/409
     */
    TenantUserCreateVO create(TenantUserCreateParam param);

    /**
     * 编辑租户用户资料和角色。
     *
     * <p>登录名不可修改；roleUids 为空时只更新资料。</p>
     *
     * @param param 编辑参数
     * @throws BusinessException 用户不存在抛出 404，参数或角色无效抛出 400
     */
    void update(TenantUserUpdateParam param);

    /**
     * 重置租户用户密码，并让目标用户当前登录态失效。
     *
     * @param param 重置密码参数
     * @return 用户 ID、登录名和一次性新密码
     * @throws BusinessException 用户不存在抛出 404，重置自己密码时抛出 400
     */
    TenantUserResetPasswordVO resetPassword(TenantUserResetPasswordParam param);

    /**
     * 启用或停用租户用户。
     *
     * <p>停用后目标用户当前登录态立即失效；不能停用当前登录账号。</p>
     *
     * @param param 启停参数
     * @throws BusinessException 用户不存在抛出 404，停用自己时抛出 400
     */
    void changeStatus(TenantUserStatusParam param);
}
