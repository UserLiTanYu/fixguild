package com.fixguild.account.service;

import com.fixguild.account.vo.UserAuthVO;
import com.fixguild.account.vo.UserInfoVO;
import com.fixguild.common.auth.LoginUser;
import com.fixguild.common.exception.BusinessException;

import java.util.List;

/**
 * 登录用户权限查询服务。
 *
 * <p>M2 直接从数据库查询用户角色、角色菜单和权限串，不引入缓存；
 * 查询结果按登录端 client_code 过滤，避免平台、租户、技师权限串用。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
public interface AuthService {

    /**
     * 查询当前用户信息。
     *
     * @param loginUser 当前登录用户
     * @return 当前用户信息
     * @throws BusinessException 用户不存在或已停用时抛出 401
     */
    UserInfoVO getUserInfo(LoginUser loginUser);

    /**
     * 查询当前用户的角色、权限和菜单树。
     *
     * @param loginUser 当前登录用户
     * @return 权限信息
     * @throws BusinessException 用户不存在或已停用时抛出 401
     */
    UserAuthVO getAuth(LoginUser loginUser);

    /**
     * 查询当前用户的角色编码集合，供 Sa-Token 鉴权使用。
     *
     * @param loginUser 当前登录用户
     * @return 角色编码集合，用户无效时返回空集合
     */
    List<String> getRoleCodes(LoginUser loginUser);

    /**
     * 查询当前用户的权限串集合，供 Sa-Token 鉴权使用。
     *
     * @param loginUser 当前登录用户
     * @return 权限串集合，用户无效时返回空集合
     */
    List<String> getPermissionCodes(LoginUser loginUser);
}
