package com.fixguild.account.service.tenant.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fixguild.account.entity.SysMenuEntity;
import com.fixguild.account.entity.SysRoleEntity;
import com.fixguild.account.entity.SysRoleMenuEntity;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.account.entity.SysUserRoleEntity;
import com.fixguild.account.mapper.SysMenuMapper;
import com.fixguild.account.mapper.SysRoleMapper;
import com.fixguild.account.mapper.SysRoleMenuMapper;
import com.fixguild.account.mapper.SysUserRoleMapper;
import com.fixguild.account.enums.DataScope;
import com.fixguild.account.service.tenant.TenantProvisionService;
import com.fixguild.account.support.RoleConstants;
import com.fixguild.common.annotation.IgnoreTenant;
import com.fixguild.common.auth.LoginContext;
import com.fixguild.common.enums.ClientCode;
import com.fixguild.common.enums.EnableStatus;
import com.fixguild.common.enums.ErrorCode;
import com.fixguild.common.enums.YesNo;
import com.fixguild.common.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户开通初始化服务实现。
 *
 * <p>新租户的默认管理员角色只绑定租户端菜单，不复制平台或技师端权限。
 * 菜单是全局表，按 client_code 查询；角色和关联表是租户表，必须显式设置 tenant_id。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantProvisionServiceImpl implements TenantProvisionService {

    /** 角色表 Mapper。 */
    private final SysRoleMapper sysRoleMapper;

    /** 菜单表 Mapper。 */
    private final SysMenuMapper sysMenuMapper;

    /** 用户角色 Mapper。 */
    private final SysUserRoleMapper sysUserRoleMapper;

    /** 角色菜单 Mapper。 */
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /**
     * {@inheritDoc}
     *
     * <p>调用方是平台端租户开通流程；新租户数据还没有进入租户上下文，
     * 因此这里显式使用 {@code @IgnoreTenant} 并手动设置新租户 tenant_id。</p>
     */
    @Override
    @IgnoreTenant
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void provisionTenantAdmin(SysUserEntity admin) {
        String tenantId = admin.getTenantId();
        SysRoleEntity role = createTenantAdminRole(tenantId);
        bindTenantMenus(tenantId, role.getId());
        bindAdminRole(tenantId, admin.getId(), role.getId());

        log.info("租户默认管理员初始化成功: tenantId={}, roleId={}, adminUserCode={}",
                tenantId, role.getId(), admin.getUserCode());
    }

    /**
     * 创建租户管理员角色。
     *
     * @param tenantId 租户编码
     * @return 已插入并带主键的角色
     */
    private SysRoleEntity createTenantAdminRole(String tenantId) {
        SysRoleEntity role = new SysRoleEntity();
        role.setUid(IdUtil.fastSimpleUUID());
        role.setTenantId(tenantId);
        role.setClientCode(ClientCode.TENANT.getCode());
        role.setRoleCode(RoleConstants.TENANT_ADMIN);
        role.setRoleName("租户管理员");
        role.setDataScope(DataScope.TENANT.getCode());
        role.setStatus(EnableStatus.ENABLED.getCode());
        role.setIsSystem(YesNo.YES.getCode());
        role.setRemark("租户开通自动创建");
        sysRoleMapper.insert(role);
        return role;
    }

    /**
     * 绑定租户端全部菜单。
     *
     * @param tenantId 租户编码
     * @param roleId   角色 ID
     */
    private void bindTenantMenus(String tenantId, Long roleId) {
        QueryWrapper query = QueryWrapper.create()
                .eq(SysMenuEntity::getClientCode, ClientCode.TENANT.getCode())
                .eq(SysMenuEntity::getStatus, EnableStatus.ENABLED.getCode());
        query.orderBy(SysMenuEntity::getSort);
        List<SysMenuEntity> menus = sysMenuMapper.selectListByQuery(query);
        if (menus.isEmpty()) {
            // 系统菜单缺失时开户不可完成，直接回滚，避免产生无权限的租户管理员
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "租户菜单未初始化");
        }

        String operator = StrUtil.blankToDefault(LoginContext.getUserCode(), "system");
        LocalDateTime now = LocalDateTime.now();
        for (SysMenuEntity menu : menus) {
            SysRoleMenuEntity roleMenu = new SysRoleMenuEntity();
            roleMenu.setTenantId(tenantId);
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menu.getId());
            roleMenu.setCreateBy(operator);
            roleMenu.setCreateTime(now);
            sysRoleMenuMapper.insert(roleMenu);
        }
    }

    /**
     * 绑定管理员和默认角色。
     *
     * @param tenantId 租户编码
     * @param userId   管理员用户 ID
     * @param roleId   角色 ID
     */
    private void bindAdminRole(String tenantId, Long userId, Long roleId) {
        SysUserRoleEntity userRole = new SysUserRoleEntity();
        userRole.setTenantId(tenantId);
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreateBy(StrUtil.blankToDefault(LoginContext.getUserCode(), "system"));
        userRole.setCreateTime(LocalDateTime.now());
        sysUserRoleMapper.insert(userRole);
    }
}
