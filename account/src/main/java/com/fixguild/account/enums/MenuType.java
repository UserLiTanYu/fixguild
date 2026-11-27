package com.fixguild.account.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 菜单节点类型。
 *
 * <p>与 sys_menu.menu_type 的 M/C/F 取值对应。</p>
 *
 * @author litanyu
 * @since 2026-11-27
 */
@Getter
@AllArgsConstructor
public enum MenuType {

    DIRECTORY("M", "目录"),
    MENU("C", "菜单"),
    BUTTON("F", "按钮");

    private final String code;

    private final String desc;
}
