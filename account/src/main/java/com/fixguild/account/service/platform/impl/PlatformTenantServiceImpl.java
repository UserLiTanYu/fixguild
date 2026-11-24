package com.fixguild.account.service.platform.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.fixguild.account.support.AccountCredentialGenerator;
import com.fixguild.account.entity.SysTenantEntity;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.account.mapper.SysTenantMapper;
import com.fixguild.account.mapper.SysUserMapper;
import com.fixguild.account.param.SysTenantCreateParam;
import com.fixguild.account.param.SysTenantPageParam;
import com.fixguild.account.param.SysTenantUpdateParam;
import com.fixguild.account.service.platform.PlatformTenantService;
import com.fixguild.account.service.tenant.TenantProvisionService;
import com.fixguild.account.vo.SysTenantCreateVO;
import com.fixguild.account.vo.SysTenantVO;
import com.fixguild.common.annotation.IgnoreTenant;
import com.fixguild.common.domain.PageResult;
import com.fixguild.common.enums.ClientCode;
import com.fixguild.common.enums.ErrorCode;
import com.fixguild.common.exception.BusinessException;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 租户管理服务实现。
 *
 * <p>租户表为平台全局表，不拼接 tenant_id；写操作统一使用
 * {@code @Transactional(rollbackFor = Exception.class)}，异常时整体回滚。
 * 手机号等敏感字段在 VO 边界脱敏，不直接返回 Entity。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformTenantServiceImpl implements PlatformTenantService {

    /** 租户表 Mapper */
    private final SysTenantMapper sysTenantMapper;

    /** 用户表 Mapper */
    private final SysUserMapper sysUserMapper;

    /** 租户开通初始化服务 */
    private final TenantProvisionService tenantProvisionService;

    /** {@inheritDoc} */
    @Override
    public PageResult<SysTenantVO> page(SysTenantPageParam param) {
        QueryWrapper query = QueryWrapper.create()
                .eq(SysTenantEntity::getStatus, param.getStatus(), param.getStatus() != null)
                .like(SysTenantEntity::getTenantName, param.getTenantName(), StrUtil.isNotBlank(param.getTenantName()))
                .eq(SysTenantEntity::getTenantCode, param.getTenantCode(), StrUtil.isNotBlank(param.getTenantCode()))
                .orderBy(SysTenantEntity::getCreateTime, false)
                // 同一时间创建时用 id 兜底，保证分页稳定
                .orderBy(SysTenantEntity::getId, false);

        Page<SysTenantEntity> page = sysTenantMapper.paginate(param.getPageNum(), param.getPageSize(), query);
        return PageResult.of(page.map(this::toVO));
    }

    /** {@inheritDoc} */
    @Override
    public SysTenantVO detail(String uid) {
        return toVO(getByUid(uid));
    }

    /** {@inheritDoc} */
    @Override
    @IgnoreTenant
    @Transactional(rollbackFor = Exception.class)
    public SysTenantCreateVO create(SysTenantCreateParam param) {
        // 先查重给出友好提示；数据库唯一索引仍是并发场景的最终兜底
        long exists = sysTenantMapper.selectCountByQuery(
                QueryWrapper.create().eq(SysTenantEntity::getTenantCode, param.getTenantCode()));
        if (exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "租户编码已存在");
        }

        SysTenantEntity entity = new SysTenantEntity();
        BeanUtil.copyProperties(param, entity);
        entity.setId(null);
        entity.setUid(IdUtil.fastSimpleUUID());
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getMaxUserCount() == null) {
            entity.setMaxUserCount(50);
        }

        try {
            sysTenantMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发插入时唯一索引冲突，转换为业务 409
            throw new BusinessException(ErrorCode.CONFLICT, "租户编码已存在");
        }

        // 平台上下文 tenant_id=0，创建租户管理员属于跨租户写入；
        // @IgnoreTenant 关闭插件自动过滤，同时下面显式指定管理员 tenant_id
        String initialPassword = AccountCredentialGenerator.generateInitialPassword();
        SysUserEntity admin = buildInitialAdmin(param, entity.getTenantCode(), initialPassword);
        try {
            sysUserMapper.insert(admin);
        } catch (DuplicateKeyException e) {
            // 用户编码或租户内登录名冲突时回滚租户，避免产生没有管理员的租户
            throw new BusinessException(ErrorCode.CONFLICT, "初始管理员创建失败，请重试");
        }
        // 同事务创建租户管理员角色并绑定租户端菜单，失败时租户和管理员一起回滚
        tenantProvisionService.provisionTenantAdmin(admin);

        log.info("创建租户成功: tenantCode={}, uid={}, adminUsername={}",
                entity.getTenantCode(), entity.getUid(), admin.getUsername());
        return toCreateVO(entity, admin, initialPassword);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysTenantUpdateParam param) {
        SysTenantEntity existing = getByUid(param.getUid());
        if (param.getTenantName() != null && StrUtil.isBlank(param.getTenantName())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "租户名称不能为空");
        }

        SysTenantEntity update = new SysTenantEntity();
        update.setId(existing.getId());
        update.setTenantName(param.getTenantName());
        update.setContactName(param.getContactName());
        update.setContactMobile(param.getContactMobile());
        update.setExpireTime(param.getExpireTime());
        update.setMaxUserCount(param.getMaxUserCount());
        update.setRemark(param.getRemark());
        // update(entity, true) 只更新非空字段；tenantCode/status 不在本接口修改
        sysTenantMapper.update(update, true);

        log.info("更新租户成功: uid={}", param.getUid());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(String uid, Integer status) {
        SysTenantEntity existing = getByUid(uid);

        SysTenantEntity update = new SysTenantEntity();
        update.setId(existing.getId());
        // 状态值由 Param 层校验；内部调用需保证只传 0/1
        update.setStatus(status);
        sysTenantMapper.update(update, true);

        log.info("租户状态变更成功: uid={}, status={}", uid, status);
    }

    /**
     * 按业务 ID 查询租户，统一处理空值和不存在场景。
     *
     * @param uid 租户业务 ID
     * @return 租户实体
     * @throws BusinessException uid 为空抛出 400，租户不存在抛出 404
     */
    private SysTenantEntity getByUid(String uid) {
        if (StrUtil.isBlank(uid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "uid 不能为空");
        }
        SysTenantEntity entity = sysTenantMapper.selectOneByQuery(
                QueryWrapper.create().eq(SysTenantEntity::getUid, uid));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "租户不存在");
        }
        return entity;
    }

    /**
     * 构造租户初始管理员。
     *
     * <p>租户管理员属于新租户，必须显式设置 tenant_id；平台端当前上下文是
     * tenant_id=0，不能依赖租户插件自动填充。</p>
     *
     * @param param           新增租户参数
     * @param tenantCode      新租户编码
     * @param initialPassword 初始密码明文
     * @return 待插入的管理员实体
     */
    private SysUserEntity buildInitialAdmin(SysTenantCreateParam param, String tenantCode, String initialPassword) {
        SysUserEntity admin = new SysUserEntity();
        admin.setUid(IdUtil.fastSimpleUUID());
        admin.setTenantId(tenantCode);
        admin.setClientCode(ClientCode.TENANT.getCode());
        // user_code 是 Sa-Token loginId，必须全局唯一
        admin.setUserCode(AccountCredentialGenerator.generateUserCode());
        admin.setUsername(param.getAdminUsername());
        admin.setPassword(AccountCredentialGenerator.hashPassword(initialPassword));
        admin.setNickname(StrUtil.blankToDefault(param.getAdminNickname(), "租户管理员"));
        admin.setStatus(1);
        return admin;
    }

    /**
     * 构造创建租户结果。
     *
     * @param tenant          租户实体
     * @param admin           初始管理员实体
     * @param initialPassword 初始密码明文
     * @return 创建结果
     */
    private SysTenantCreateVO toCreateVO(SysTenantEntity tenant, SysUserEntity admin, String initialPassword) {
        SysTenantCreateVO vo = new SysTenantCreateVO();
        vo.setUid(tenant.getUid());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setAdminUsername(admin.getUsername());
        vo.setAdminUserUid(admin.getUid());
        vo.setInitialPassword(initialPassword);
        return vo;
    }

    /**
     * 实体转 VO，并在输出边界完成手机号脱敏。
     *
     * @param entity 租户实体
     * @return 租户 VO
     */
    private SysTenantVO toVO(SysTenantEntity entity) {
        SysTenantVO vo = new SysTenantVO();
        BeanUtil.copyProperties(entity, vo);
        // 手机号在 VO 边界脱敏，避免 Entity 或明文直接出接口
        if (StrUtil.isNotBlank(entity.getContactMobile())) {
            vo.setContactMobile(DesensitizedUtil.mobilePhone(entity.getContactMobile()));
        }
        return vo;
    }
}
