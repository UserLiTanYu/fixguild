package com.fixguild.account.service.tenant.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fixguild.account.entity.SysRoleEntity;
import com.fixguild.account.entity.SysTenantEntity;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.account.entity.SysUserRoleEntity;
import com.fixguild.account.mapper.SysRoleMapper;
import com.fixguild.account.mapper.SysTenantMapper;
import com.fixguild.account.mapper.SysUserMapper;
import com.fixguild.account.mapper.SysUserRoleMapper;
import com.fixguild.account.param.TenantUserCreateParam;
import com.fixguild.account.param.TenantUserPageParam;
import com.fixguild.account.param.TenantUserResetPasswordParam;
import com.fixguild.account.param.TenantUserStatusParam;
import com.fixguild.account.param.TenantUserUpdateParam;
import com.fixguild.account.security.LoginAttemptGuard;
import com.fixguild.account.service.tenant.TenantUserService;
import com.fixguild.account.support.AccountCredentialGenerator;
import com.fixguild.account.vo.TenantUserCreateVO;
import com.fixguild.account.vo.TenantUserResetPasswordVO;
import com.fixguild.account.vo.TenantUserVO;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户端用户管理服务实现。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantUserServiceImpl implements TenantUserService {

    /** 用户表 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /** 用户角色 Mapper。 */
    private final SysUserRoleMapper sysUserRoleMapper;

    /** 角色表 Mapper。 */
    private final SysRoleMapper sysRoleMapper;

    /** 租户表 Mapper。 */
    private final SysTenantMapper sysTenantMapper;

    /** 登录失败锁定器。 */
    private final LoginAttemptGuard loginAttemptGuard;

    /** {@inheritDoc} */
    @Override
    public PageResult<TenantUserVO> page(TenantUserPageParam param) {
        String tenantId = TenantContext.requireTenantId();
        QueryWrapper query = QueryWrapper.create()
                .eq(SysUserEntity::getTenantId, tenantId)
                .eq(SysUserEntity::getClientCode, ClientCode.TENANT.getCode())
                .like(SysUserEntity::getUsername, param.getUsername(), StrUtil.isNotBlank(param.getUsername()))
                .like(SysUserEntity::getNickname, param.getNickname(), StrUtil.isNotBlank(param.getNickname()))
                .eq(SysUserEntity::getStatus, param.getStatus(), param.getStatus() != null)
                .orderBy(SysUserEntity::getCreateTime, false)
                .orderBy(SysUserEntity::getId, false);

        Page<SysUserEntity> page = sysUserMapper.paginate(param.getPageNum(), param.getPageSize(), query);
        Map<Long, List<SysRoleEntity>> rolesByUser = loadRoles(tenantId, page.getRecords());
        return PageResult.of(page.map(user -> toVO(user, rolesByUser.get(user.getId()))));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantUserCreateVO create(TenantUserCreateParam param) {
        String tenantId = TenantContext.requireTenantId();
        String username = StrUtil.trim(param.getUsername());
        checkMaxUserCount(tenantId);
        checkUsernameUnique(tenantId, username);
        List<SysRoleEntity> roles = validateRoles(tenantId, param.getRoleUids());

        String initialPassword = AccountCredentialGenerator.generateInitialPassword();
        SysUserEntity user = new SysUserEntity();
        user.setUid(IdUtil.fastSimpleUUID());
        user.setUserCode(AccountCredentialGenerator.generateUserCode());
        user.setTenantId(tenantId);
        user.setClientCode(ClientCode.TENANT.getCode());
        user.setUsername(username);
        user.setPassword(AccountCredentialGenerator.hashPassword(initialPassword));
        user.setNickname(StrUtil.trim(param.getNickname()));
        user.setEmail(StrUtil.isBlank(param.getEmail()) ? null : StrUtil.trim(param.getEmail()));
        user.setStatus(1);
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "登录名已存在");
        }
        bindRoles(tenantId, user.getId(), roles);

        log.info("创建租户用户成功: tenantId={}, username={}, uid={}",
                tenantId, user.getUsername(), user.getUid());
        return new TenantUserCreateVO(user.getUid(), user.getUsername(), initialPassword);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TenantUserUpdateParam param) {
        String tenantId = TenantContext.requireTenantId();
        SysUserEntity user = getByUid(param.getUid(), tenantId);
        if (param.getNickname() != null && StrUtil.isBlank(param.getNickname())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称不能为空");
        }
        boolean hasProfile = param.getNickname() != null || param.getEmail() != null;
        boolean hasRoles = param.getRoleUids() != null;
        if (!hasProfile && !hasRoles) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "没有需要更新的字段");
        }
        if (hasRoles && param.getRoleUids().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一个角色");
        }
        List<SysRoleEntity> roles = hasRoles ? validateRoles(tenantId, param.getRoleUids()) : List.of();

        if (hasProfile) {
            SysUserEntity update = new SysUserEntity();
            update.setId(user.getId());
            update.setNickname(StrUtil.trim(param.getNickname()));
            update.setEmail(normalizeEmail(param.getEmail()));
            sysUserMapper.update(update, true);
        }
        if (hasRoles) {
            sysUserRoleMapper.deleteByQuery(QueryWrapper.create()
                    .eq(SysUserRoleEntity::getTenantId, tenantId)
                    .eq(SysUserRoleEntity::getUserId, user.getId()));
            bindRoles(tenantId, user.getId(), roles);
        }

        log.info("更新租户用户成功: tenantId={}, username={}, uid={}",
                tenantId, user.getUsername(), user.getUid());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantUserResetPasswordVO resetPassword(TenantUserResetPasswordParam param) {
        String tenantId = TenantContext.requireTenantId();
        SysUserEntity user = getByUid(param.getUid(), tenantId);
        if (user.getUserCode().equals(LoginContext.getUserCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请使用修改密码接口重置自己的密码");
        }

        String newPassword = AccountCredentialGenerator.generateInitialPassword();
        SysUserEntity update = new SysUserEntity();
        update.setId(user.getId());
        update.setPassword(AccountCredentialGenerator.hashPassword(newPassword));
        sysUserMapper.update(update, true);
        clearLoginFailures(tenantId, user.getUsername());
        StpUtil.logout(user.getUserCode());

        log.info("重置租户用户密码成功: tenantId={}, username={}, uid={}",
                tenantId, user.getUsername(), user.getUid());
        return new TenantUserResetPasswordVO(user.getUid(), user.getUsername(), newPassword);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(TenantUserStatusParam param) {
        String tenantId = TenantContext.requireTenantId();
        SysUserEntity user = getByUid(param.getUid(), tenantId);
        if (Integer.valueOf(0).equals(param.getStatus())
                && user.getUserCode().equals(LoginContext.getUserCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能停用当前登录账号");
        }

        SysUserEntity update = new SysUserEntity();
        update.setId(user.getId());
        update.setStatus(param.getStatus());
        sysUserMapper.update(update, true);
        if (Integer.valueOf(0).equals(param.getStatus())) {
            StpUtil.logout(user.getUserCode());
        } else {
            clearLoginFailures(tenantId, user.getUsername());
        }

        log.info("变更租户用户状态成功: tenantId={}, username={}, status={}",
                tenantId, user.getUsername(), param.getStatus());
    }

    /**
     * 校验用户数量上限。
     *
     * @param tenantId 租户编码
     */
    private void checkMaxUserCount(String tenantId) {
        SysTenantEntity tenant = sysTenantMapper.selectOneByQuery(QueryWrapper.create()
                .eq(SysTenantEntity::getTenantCode, tenantId));
        if (tenant == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "租户不存在");
        }
        if (tenant.getMaxUserCount() == null) {
            return;
        }
        long count = sysUserMapper.selectCountByQuery(QueryWrapper.create()
                .eq(SysUserEntity::getTenantId, tenantId)
                .eq(SysUserEntity::getClientCode, ClientCode.TENANT.getCode()));
        if (count >= tenant.getMaxUserCount()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "租户用户数已达上限");
        }
    }

    /**
     * 校验登录名在当前租户内唯一。
     *
     * @param tenantId 租户编码
     * @param username 登录名
     */
    private void checkUsernameUnique(String tenantId, String username) {
        long count = sysUserMapper.selectCountByQuery(QueryWrapper.create()
                .eq(SysUserEntity::getTenantId, tenantId)
                .eq(SysUserEntity::getClientCode, ClientCode.TENANT.getCode())
                .eq(SysUserEntity::getUsername, username));
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "登录名已存在");
        }
    }

    /**
     * 校验角色都是当前租户有效租户端角色。
     *
     * @param tenantId 租户编码
     * @param roleUids 角色业务 ID 列表
     * @return 角色实体列表
     */
    private List<SysRoleEntity> validateRoles(String tenantId, List<String> roleUids) {
        if (roleUids == null || roleUids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一个角色");
        }
        Set<String> uids = roleUids.stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (uids.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "至少选择一个角色");
        }
        List<SysRoleEntity> roles = sysRoleMapper.selectListByQuery(QueryWrapper.create()
                .eq(SysRoleEntity::getTenantId, tenantId)
                .eq(SysRoleEntity::getClientCode, ClientCode.TENANT.getCode())
                .eq(SysRoleEntity::getStatus, 1)
                .in(SysRoleEntity::getUid, uids));
        if (roles.size() != uids.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "包含无效角色");
        }
        return roles;
    }

    /**
     * 绑定用户角色。
     *
     * @param tenantId 租户编码
     * @param userId   用户 ID
     * @param roles    角色列表
     */
    private void bindRoles(String tenantId, Long userId, List<SysRoleEntity> roles) {
        String operator = StrUtil.blankToDefault(LoginContext.getUserCode(), "system");
        LocalDateTime now = LocalDateTime.now();
        for (SysRoleEntity role : roles) {
            SysUserRoleEntity userRole = new SysUserRoleEntity();
            userRole.setTenantId(tenantId);
            userRole.setUserId(userId);
            userRole.setRoleId(role.getId());
            userRole.setCreateBy(operator);
            userRole.setCreateTime(now);
            sysUserRoleMapper.insert(userRole);
        }
    }

    /**
     * 批量加载用户角色，避免分页逐条查询。
     *
     * @param tenantId 租户编码
     * @param users    用户列表
     * @return 用户 ID → 角色列表
     */
    private Map<Long, List<SysRoleEntity>> loadRoles(String tenantId, List<SysUserEntity> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = users.stream().map(SysUserEntity::getId).toList();
        List<SysUserRoleEntity> userRoles = sysUserRoleMapper.selectListByQuery(QueryWrapper.create()
                .eq(SysUserRoleEntity::getTenantId, tenantId)
                .in(SysUserRoleEntity::getUserId, userIds));
        Set<Long> roleIds = userRoles.stream()
                .map(SysUserRoleEntity::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        List<SysRoleEntity> roles = sysRoleMapper.selectListByQuery(QueryWrapper.create()
                .eq(SysRoleEntity::getTenantId, tenantId)
                .eq(SysRoleEntity::getClientCode, ClientCode.TENANT.getCode())
                .in(SysRoleEntity::getId, roleIds));
        Map<Long, SysRoleEntity> roleById = roles.stream()
                .collect(Collectors.toMap(SysRoleEntity::getId, role -> role, (left, right) -> left));

        Map<Long, List<SysRoleEntity>> result = new LinkedHashMap<>();
        for (SysUserRoleEntity userRole : userRoles) {
            SysRoleEntity role = roleById.get(userRole.getRoleId());
            if (role != null) {
                result.computeIfAbsent(userRole.getUserId(), key -> new ArrayList<>()).add(role);
            }
        }
        return result;
    }

    /**
     * 按业务 ID 查询当前租户用户。
     *
     * @param uid      用户业务 ID
     * @param tenantId 租户编码
     * @return 用户实体
     */
    private SysUserEntity getByUid(String uid, String tenantId) {
        if (StrUtil.isBlank(uid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户 ID 不能为空");
        }
        SysUserEntity user = sysUserMapper.selectOneByQuery(QueryWrapper.create()
                .eq(SysUserEntity::getUid, uid)
                .eq(SysUserEntity::getTenantId, tenantId)
                .eq(SysUserEntity::getClientCode, ClientCode.TENANT.getCode()));
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 清除用户登录失败记录。
     *
     * @param tenantId 租户编码
     * @param username 登录名
     */
    private void clearLoginFailures(String tenantId, String username) {
        loginAttemptGuard.clear(ClientCode.TENANT, tenantId, username);
    }

    /**
     * 规范化更新时的邮箱：null 表示不更新，空字符串表示清空。
     *
     * @param email 邮箱
     * @return 规范化邮箱
     */
    private String normalizeEmail(String email) {
        return email == null ? null : StrUtil.trim(email);
    }

    /**
     * 实体转 VO。
     *
     * @param user  用户实体
     * @param roles 用户角色
     * @return 用户 VO
     */
    private TenantUserVO toVO(SysUserEntity user, List<SysRoleEntity> roles) {
        TenantUserVO vo = new TenantUserVO();
        BeanUtil.copyProperties(user, vo);
        List<SysRoleEntity> actualRoles = roles == null ? List.of() : roles;
        vo.setRoleUids(actualRoles.stream().map(SysRoleEntity::getUid).toList());
        vo.setRoleNames(actualRoles.stream().map(SysRoleEntity::getRoleName).toList());
        return vo;
    }
}
