package com.fixguild.account.service;

import com.fixguild.account.entity.SysUserEntity;

/**
 * 租户开通初始化服务。
 *
 * <p>负责给新租户创建默认管理员角色，并绑定租户端菜单和管理员账号。
 * 调用方必须处于事务中，保证租户、管理员、角色和菜单绑定一起提交或回滚。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
public interface TenantProvisionService {

    /**
     * 为租户初始管理员创建默认角色和菜单权限。
     *
     * @param admin 已插入并带有主键的初始管理员
     */
    void provisionTenantAdmin(SysUserEntity admin);
}
