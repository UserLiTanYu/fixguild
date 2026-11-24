package com.fixguild.account.support;

import com.fixguild.account.entity.SysMenuEntity;
import com.fixguild.account.vo.MenuTreeVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单树构建器。
 *
 * <p>菜单通过 parent_id 组成树；权限接口可以排除按钮节点，角色分配菜单时需要
 * 包含按钮节点。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
public final class MenuTreeBuilder {

    /** 按钮菜单类型。 */
    private static final String MENU_TYPE_BUTTON = "F";

    private MenuTreeBuilder() {
    }

    /**
     * 将菜单列表组装成树。
     *
     * @param menus          菜单列表，按期望的显示顺序传入
     * @param includeButtons 是否包含按钮节点
     * @return 根菜单列表
     */
    public static List<MenuTreeVO> build(List<SysMenuEntity> menus, boolean includeButtons) {
        Map<Long, MenuTreeVO> nodeById = new LinkedHashMap<>();
        for (SysMenuEntity menu : menus) {
            if (!includeButtons && MENU_TYPE_BUTTON.equals(menu.getMenuType())) {
                continue;
            }
            MenuTreeVO node = new MenuTreeVO();
            node.setUid(menu.getUid());
            node.setMenuName(menu.getMenuName());
            node.setMenuType(menu.getMenuType());
            node.setPath(menu.getPath());
            node.setComponent(menu.getComponent());
            node.setPerms(menu.getPerms());
            node.setIcon(menu.getIcon());
            node.setSort(menu.getSort());
            nodeById.put(menu.getId(), node);
        }

        List<MenuTreeVO> roots = new ArrayList<>();
        for (SysMenuEntity menu : menus) {
            MenuTreeVO node = nodeById.get(menu.getId());
            if (node == null) {
                continue;
            }
            MenuTreeVO parent = menu.getParentId() == null ? null : nodeById.get(menu.getParentId());
            node.setParentUid(parent == null ? null : parent.getUid());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
