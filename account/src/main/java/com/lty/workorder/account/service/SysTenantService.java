package com.lty.workorder.account.service;

import com.lty.workorder.account.param.SysTenantCreateParam;
import com.lty.workorder.account.param.SysTenantPageParam;
import com.lty.workorder.account.param.SysTenantUpdateParam;
import com.lty.workorder.account.vo.SysTenantVO;
import com.lty.workorder.common.domain.PageResult;
import com.lty.workorder.common.exception.BusinessException;

/**
 * 租户管理服务。
 *
 * <p>租户表是平台级全局表，不参与租户数据隔离；写操作在事务内完成。
 * 租户编码平台内唯一，业务表的 tenant_id 存 tenantCode。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
public interface SysTenantService {

    /**
     * 分页查询租户。
     *
     * <p>筛选条件为空时不过滤；按创建时间倒序，同一时间用 id 兜底保证分页稳定。</p>
     *
     * @param param 分页与筛选参数
     * @return 租户分页结果
     */
    PageResult<SysTenantVO> page(SysTenantPageParam param);

    /**
     * 查询租户详情。
     *
     * @param uid 租户业务 ID
     * @return 租户详情，手机号已脱敏
     * @throws BusinessException uid 为空抛出 400，租户不存在抛出 404
     */
    SysTenantVO detail(String uid);

    /**
     * 新增租户。
     *
     * <p>租户编码平台内唯一；状态为空时默认启用，最大用户数为空时默认 50。</p>
     *
     * @param param 新增参数
     * @return 租户业务 ID
     * @throws BusinessException 租户编码重复时抛出 409
     */
    String create(SysTenantCreateParam param);

    /**
     * 编辑租户。
     *
     * <p>只更新非空字段；租户编码不可修改，状态变更走独立接口。</p>
     *
     * @param param 编辑参数
     * @throws BusinessException uid 为空抛出 400，租户不存在抛出 404
     */
    void update(SysTenantUpdateParam param);

    /**
     * 启用或停用租户。
     *
     * <p>状态值只允许 0/1，不校验状态流转；重复启停是幂等的。</p>
     *
     * @param uid    租户业务 ID
     * @param status 1 启用，0 停用
     * @throws BusinessException uid 为空抛出 400，租户不存在抛出 404
     */
    void changeStatus(String uid, Integer status);
}
