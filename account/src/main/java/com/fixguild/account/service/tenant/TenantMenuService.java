package com.fixguild.account.service.tenant;

import com.fixguild.account.vo.MenuTreeVO;

import java.util.List;

/**
 * 菜单查询服务。
 *
 * @author litanyu
 * @since 2026-11-24
 */
public interface TenantMenuService {

    /**
     * 查询指定登录端的完整菜单树，包含按钮节点，供角色分配菜单使用。
     *
     * @param clientCode 登录端编码
     * @return 菜单树
     */
    List<MenuTreeVO> tree(String clientCode);
}
