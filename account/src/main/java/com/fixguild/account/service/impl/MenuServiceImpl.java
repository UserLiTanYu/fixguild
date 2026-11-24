package com.fixguild.account.service.impl;

import com.fixguild.account.entity.SysMenuEntity;
import com.fixguild.account.mapper.SysMenuMapper;
import com.fixguild.account.service.MenuService;
import com.fixguild.account.support.MenuTreeBuilder;
import com.fixguild.account.vo.MenuTreeVO;
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
public class MenuServiceImpl implements MenuService {

    /** 菜单 Mapper。 */
    private final SysMenuMapper sysMenuMapper;

    /** {@inheritDoc} */
    @Override
    public List<MenuTreeVO> tree(String clientCode) {
        QueryWrapper query = QueryWrapper.create()
                .eq(SysMenuEntity::getClientCode, clientCode)
                .eq(SysMenuEntity::getStatus, 1);
        query.orderBy(SysMenuEntity::getSort);
        List<SysMenuEntity> menus = sysMenuMapper.selectListByQuery(query);
        return MenuTreeBuilder.build(menus, true);
    }
}
