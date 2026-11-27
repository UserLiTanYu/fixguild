package com.fixguild.account.service.tenant.impl;

import com.fixguild.account.entity.SysMenuEntity;
import com.fixguild.account.mapper.SysMenuMapper;
import com.fixguild.account.service.tenant.TenantMenuService;
import com.fixguild.account.support.MenuTreeBuilder;
import com.fixguild.account.vo.MenuTreeVO;
import com.fixguild.common.enums.EnableStatus;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单查询服务实现。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Service
@RequiredArgsConstructor
public class TenantMenuServiceImpl implements TenantMenuService {

    /** 菜单 Mapper。 */
    private final SysMenuMapper sysMenuMapper;

    /** {@inheritDoc} */
    @Override
    public List<MenuTreeVO> tree(String clientCode) {
        QueryWrapper query = QueryWrapper.create()
                .eq(SysMenuEntity::getClientCode, clientCode)
                .eq(SysMenuEntity::getStatus, EnableStatus.ENABLED.getCode());
        query.orderBy(SysMenuEntity::getSort);
        List<SysMenuEntity> menus = sysMenuMapper.selectListByQuery(query);
        return MenuTreeBuilder.build(menus, true);
    }
}
