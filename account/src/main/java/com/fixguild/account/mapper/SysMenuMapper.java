package com.fixguild.account.mapper;

import com.fixguild.account.entity.SysMenuEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单/权限 Mapper。
 *
 * <p>sys_menu 是全局表，已在租户插件 ignore-tables 中配置。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuEntity> {
}
