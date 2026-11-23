package com.lty.workorder.account.mapper;

import com.lty.workorder.account.entity.SysTenantEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 租户表 Mapper。
 *
 * <p>继承 MyBatis-Flex BaseMapper，当前没有自定义 SQL；租户表是平台全局表，
 * 不参与租户插件过滤。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenantEntity> {
}
