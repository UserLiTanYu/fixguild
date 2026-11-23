package com.fixguild.web;

import com.fixguild.account.mapper.SysUserMapper;
import com.fixguild.common.annotation.IgnoreTenant;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 仅测试使用的租户查询服务，用于验证 @IgnoreTenant。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class TestTenantQueryService {

    /** 用户 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /**
     * 统计所有租户的用户数。
     *
     * @return 用户数量
     */
    @IgnoreTenant
    public long countAllUsers() {
        return sysUserMapper.selectCountByQuery(QueryWrapper.create());
    }

    /**
     * 统计当前租户的用户数。
     *
     * @return 用户数量
     */
    public long countCurrentTenantUsers() {
        return sysUserMapper.selectCountByQuery(QueryWrapper.create());
    }
}
