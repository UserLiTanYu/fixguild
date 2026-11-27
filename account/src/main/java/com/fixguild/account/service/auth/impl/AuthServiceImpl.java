package com.fixguild.account.service.auth.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fixguild.account.entity.SysMenuEntity;
import com.fixguild.account.entity.SysRoleEntity;
import com.fixguild.account.entity.SysRoleMenuEntity;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.account.entity.SysUserRoleEntity;
import com.fixguild.account.mapper.SysMenuMapper;
import com.fixguild.account.mapper.SysRoleMapper;
import com.fixguild.account.mapper.SysRoleMenuMapper;
import com.fixguild.account.mapper.SysUserMapper;
import com.fixguild.account.mapper.SysUserRoleMapper;
import com.fixguild.account.service.auth.AuthService;
import com.fixguild.account.support.MenuTreeBuilder;
import com.fixguild.account.vo.MenuTreeVO;
import com.fixguild.account.vo.UserAuthVO;
import com.fixguild.account.vo.UserInfoVO;
import com.fixguild.common.auth.LoginUser;
import com.fixguild.common.enums.EnableStatus;
import com.fixguild.common.enums.ErrorCode;
import com.fixguild.common.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录用户权限查询服务实现。
 *
 * <p>查询链路为 sys_user → sys_user_role → sys_role → sys_role_menu → sys_menu。
 * M2 不缓存权限，每次鉴权直接查库；角色和菜单都必须处于启用状态。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 空权限快照，避免重复创建空集合。 */
    private static final AuthSnapshot EMPTY = new AuthSnapshot(List.of(), List.of(), List.of());

    /** 用户 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /** 用户角色 Mapper。 */
    private final SysUserRoleMapper sysUserRoleMapper;

    /** 角色 Mapper。 */
    private final SysRoleMapper sysRoleMapper;

    /** 角色菜单 Mapper。 */
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /** 菜单 Mapper。 */
    private final SysMenuMapper sysMenuMapper;

    /** {@inheritDoc} */
    @Override
    public UserInfoVO getUserInfo(LoginUser loginUser) {
        SysUserEntity user = requireUser(loginUser);
        UserInfoVO vo = new UserInfoVO();
        BeanUtil.copyProperties(user, vo);
        return vo;
    }

    /** {@inheritDoc} */
    @Override
    public UserAuthVO getAuth(LoginUser loginUser) {
        AuthSnapshot snapshot = loadSnapshot(requireUser(loginUser));
        UserAuthVO vo = new UserAuthVO();
        vo.setRoles(snapshot.roles());
        vo.setPermissions(snapshot.permissions());
        vo.setMenus(snapshot.menus());
        return vo;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getRoleCodes(LoginUser loginUser) {
        SysUserEntity user = findUser(loginUser);
        return user == null ? List.of() : loadSnapshot(user).roles();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getPermissionCodes(LoginUser loginUser) {
        SysUserEntity user = findUser(loginUser);
        return user == null ? List.of() : loadSnapshot(user).permissions();
    }

    /**
     * 查询并校验当前登录用户。
     *
     * @param loginUser 当前登录用户
     * @return 用户实体
     * @throws BusinessException 用户不存在或已停用时抛出 401
     */
    private SysUserEntity requireUser(LoginUser loginUser) {
        SysUserEntity user = findUser(loginUser);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已停用");
        }
        return user;
    }

    /**
     * 按用户编码、租户和登录端查询启用用户。
     *
     * @param loginUser 当前登录用户
     * @return 用户实体，不存在时返回 null
     */
    private SysUserEntity findUser(LoginUser loginUser) {
        return sysUserMapper.selectOneByQuery(QueryWrapper.create()
                .eq(SysUserEntity::getUserCode, loginUser.userCode())
                .eq(SysUserEntity::getTenantId, loginUser.tenantId())
                .eq(SysUserEntity::getClientCode, loginUser.clientCode())
                .eq(SysUserEntity::getStatus, EnableStatus.ENABLED.getCode()));
    }

    /**
     * 加载角色、权限和菜单快照。
     *
     * @param user 当前用户
     * @return 权限快照
     */
    private AuthSnapshot loadSnapshot(SysUserEntity user) {
        List<SysUserRoleEntity> userRoles = sysUserRoleMapper.selectListByQuery(QueryWrapper.create()
                .eq(SysUserRoleEntity::getTenantId, user.getTenantId())
                .eq(SysUserRoleEntity::getUserId, user.getId()));
        Set<Long> roleIds = userRoles.stream()
                .map(SysUserRoleEntity::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            return EMPTY;
        }

        QueryWrapper roleQuery = QueryWrapper.create()
                .eq(SysRoleEntity::getTenantId, user.getTenantId())
                .eq(SysRoleEntity::getClientCode, user.getClientCode())
                .eq(SysRoleEntity::getStatus, EnableStatus.ENABLED.getCode())
                .in(SysRoleEntity::getId, roleIds);
        roleQuery.orderBy(SysRoleEntity::getId);
        List<SysRoleEntity> roles = sysRoleMapper.selectListByQuery(roleQuery);
        if (roles.isEmpty()) {
            return EMPTY;
        }

        List<String> roleCodes = roles.stream()
                .map(SysRoleEntity::getRoleCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        Set<Long> activeRoleIds = roles.stream()
                .map(SysRoleEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (activeRoleIds.isEmpty()) {
            return new AuthSnapshot(roleCodes, List.of(), List.of());
        }

        List<SysRoleMenuEntity> roleMenus = sysRoleMenuMapper.selectListByQuery(QueryWrapper.create()
                .eq(SysRoleMenuEntity::getTenantId, user.getTenantId())
                .in(SysRoleMenuEntity::getRoleId, activeRoleIds));
        Set<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenuEntity::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (menuIds.isEmpty()) {
            return new AuthSnapshot(roleCodes, List.of(), List.of());
        }

        QueryWrapper menuQuery = QueryWrapper.create()
                .in(SysMenuEntity::getId, menuIds)
                .eq(SysMenuEntity::getClientCode, user.getClientCode())
                .eq(SysMenuEntity::getStatus, EnableStatus.ENABLED.getCode());
        menuQuery.orderBy(SysMenuEntity::getSort);
        List<SysMenuEntity> menus = sysMenuMapper.selectListByQuery(menuQuery);

        List<String> permissions = menus.stream()
                .map(SysMenuEntity::getPerms)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        return new AuthSnapshot(roleCodes, permissions, MenuTreeBuilder.build(menus, false));
    }

    /**
     * 权限快照。
     *
     * @param roles       角色编码
     * @param permissions 权限串
     * @param menus       菜单树
     */
    private record AuthSnapshot(List<String> roles, List<String> permissions, List<MenuTreeVO> menus) {
    }
}
