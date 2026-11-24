package com.fixguild.account.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fixguild.account.entity.SysMenuEntity;
import com.fixguild.account.entity.SysRoleEntity;
import com.fixguild.account.entity.SysRoleMenuEntity;
import com.fixguild.account.mapper.SysMenuMapper;
import com.fixguild.account.mapper.SysRoleMapper;
import com.fixguild.account.mapper.SysRoleMenuMapper;
import com.fixguild.account.param.SysRoleCreateParam;
import com.fixguild.account.param.SysRoleMenusParam;
import com.fixguild.account.param.SysRolePageParam;
import com.fixguild.account.param.SysRoleUpdateParam;
import com.fixguild.account.service.SysRoleService;
import com.fixguild.account.vo.SysRoleVO;
import com.fixguild.common.auth.LoginContext;
import com.fixguild.common.domain.PageResult;
import com.fixguild.common.enums.ClientCode;
import com.fixguild.common.enums.ErrorCode;
import com.fixguild.common.exception.BusinessException;
import com.fixguild.common.tenant.TenantContext;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户端角色管理服务实现。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    /** M2 租户角色数据范围：本租户。 */
    private static final int TENANT_DATA_SCOPE = 2;

    /** 角色表 Mapper。 */
    private final SysRoleMapper sysRoleMapper;

    /** 菜单表 Mapper。 */
    private final SysMenuMapper sysMenuMapper;

    /** 角色菜单 Mapper。 */
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /** {@inheritDoc} */
    @Override
    public PageResult<SysRoleVO> page(SysRolePageParam param) {
        String tenantId = currentTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq(SysRoleEntity::getTenantId, tenantId)
                .eq(SysRoleEntity::getClientCode, ClientCode.TENANT.getCode())
                .eq(SysRoleEntity::getRoleCode, param.getRoleCode(), StrUtil.isNotBlank(param.getRoleCode()))
                .like(SysRoleEntity::getRoleName, param.getRoleName(), StrUtil.isNotBlank(param.getRoleName()))
                .eq(SysRoleEntity::getStatus, param.getStatus(), param.getStatus() != null)
                .orderBy(SysRoleEntity::getIsSystem, false)
                .orderBy(SysRoleEntity::getCreateTime, false)
                .orderBy(SysRoleEntity::getId, false);

        Page<SysRoleEntity> page = sysRoleMapper.paginate(param.getPageNum(), param.getPageSize(), query);
        return PageResult.of(page.map(this::toVO));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SysRoleCreateParam param) {
        String tenantId = currentTenantId();
        String roleCode = StrUtil.trim(param.getRoleCode());
        long exists = sysRoleMapper.selectCountByQuery(QueryWrapper.create()
                .eq(SysRoleEntity::getTenantId, tenantId)
                .eq(SysRoleEntity::getClientCode, ClientCode.TENANT.getCode())
                .eq(SysRoleEntity::getRoleCode, roleCode));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码已存在");
        }
        checkDataScope(param.getDataScope());

        SysRoleEntity role = new SysRoleEntity();
        role.setUid(IdUtil.fastSimpleUUID());
        role.setTenantId(tenantId);
        role.setClientCode(ClientCode.TENANT.getCode());
        role.setRoleCode(roleCode);
        role.setRoleName(StrUtil.trim(param.getRoleName()));
        role.setDataScope(param.getDataScope() == null ? TENANT_DATA_SCOPE : param.getDataScope());
        role.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        role.setIsSystem(0);
        role.setRemark(param.getRemark());
        try {
            sysRoleMapper.insert(role);
        } catch (DuplicateKeyException e) {
            // 并发创建时数据库唯一索引兜底
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码已存在");
        }

        log.info("创建角色成功: tenantId={}, roleCode={}, uid={}",
                tenantId, role.getRoleCode(), role.getUid());
        return role.getUid();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysRoleUpdateParam param) {
        String tenantId = currentTenantId();
        SysRoleEntity role = getByUid(param.getUid(), tenantId);
        if (Integer.valueOf(1).equals(role.getIsSystem())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "系统内置角色不允许修改");
        }
        if (param.getRoleName() != null && StrUtil.isBlank(param.getRoleName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色名称不能为空");
        }
        if (param.getRoleName() == null && param.getDataScope() == null
                && param.getStatus() == null && param.getRemark() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "没有需要更新的字段");
        }
        checkDataScope(param.getDataScope());

        SysRoleEntity update = new SysRoleEntity();
        update.setId(role.getId());
        update.setRoleName(StrUtil.trim(param.getRoleName()));
        update.setDataScope(param.getDataScope());
        update.setStatus(param.getStatus());
        update.setRemark(param.getRemark());
        sysRoleMapper.update(update, true);

        log.info("更新角色成功: tenantId={}, roleCode={}, uid={}",
                tenantId, role.getRoleCode(), role.getUid());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindMenus(SysRoleMenusParam param) {
        String tenantId = currentTenantId();
        SysRoleEntity role = getByUid(param.getUid(), tenantId);
        if (Integer.valueOf(1).equals(role.getIsSystem())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "系统内置角色不允许修改菜单");
        }
        if (param.getMenuUids() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜单列表不能为空");
        }

        Set<String> menuUids = param.getMenuUids().stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        sysRoleMenuMapper.deleteByQuery(QueryWrapper.create()
                .eq(SysRoleMenuEntity::getTenantId, tenantId)
                .eq(SysRoleMenuEntity::getRoleId, role.getId()));
        if (menuUids.isEmpty()) {
            log.info("清空角色菜单: tenantId={}, roleUid={}", tenantId, role.getUid());
            return;
        }

        List<SysMenuEntity> menus = sysMenuMapper.selectListByQuery(QueryWrapper.create()
                .in(SysMenuEntity::getUid, menuUids)
                .eq(SysMenuEntity::getClientCode, ClientCode.TENANT.getCode())
                .eq(SysMenuEntity::getStatus, 1));
        if (menus.size() != menuUids.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "包含无效菜单");
        }

        String operator = StrUtil.blankToDefault(LoginContext.getUserCode(), "system");
        LocalDateTime now = LocalDateTime.now();
        for (SysMenuEntity menu : menus) {
            SysRoleMenuEntity roleMenu = new SysRoleMenuEntity();
            roleMenu.setTenantId(tenantId);
            roleMenu.setRoleId(role.getId());
            roleMenu.setMenuId(menu.getId());
            roleMenu.setCreateBy(operator);
            roleMenu.setCreateTime(now);
            sysRoleMenuMapper.insert(roleMenu);
        }
        log.info("分配角色菜单成功: tenantId={}, roleUid={}, menuCount={}",
                tenantId, role.getUid(), menus.size());
    }

    /**
     * 按业务 ID 查询当前租户的租户端角色。
     *
     * @param uid      角色业务 ID
     * @param tenantId 租户编码
     * @return 角色实体
     */
    private SysRoleEntity getByUid(String uid, String tenantId) {
        if (StrUtil.isBlank(uid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色 ID 不能为空");
        }
        SysRoleEntity role = sysRoleMapper.selectOneByQuery(QueryWrapper.create()
                .eq(SysRoleEntity::getUid, uid)
                .eq(SysRoleEntity::getTenantId, tenantId)
                .eq(SysRoleEntity::getClientCode, ClientCode.TENANT.getCode()));
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return role;
    }

    /**
     * 获取当前租户编码。
     *
     * @return 租户编码
     * @throws BusinessException 登录会话缺少租户信息时抛出 401
     */
    private String currentTenantId() {
        String tenantId = TenantContext.get();
        if (StrUtil.isBlank(tenantId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录会话缺少租户信息");
        }
        return tenantId;
    }

    /**
     * 校验租户角色数据范围。
     *
     * @param dataScope 数据范围
     */
    private void checkDataScope(Integer dataScope) {
        if (dataScope != null && dataScope != TENANT_DATA_SCOPE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "租户角色数据范围只能是 2（本租户）");
        }
    }

    /**
     * 实体转 VO。
     *
     * @param entity 角色实体
     * @return 角色 VO
     */
    private SysRoleVO toVO(SysRoleEntity entity) {
        SysRoleVO vo = new SysRoleVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }
}
