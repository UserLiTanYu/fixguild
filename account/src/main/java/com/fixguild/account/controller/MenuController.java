package com.fixguild.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fixguild.account.service.MenuService;
import com.fixguild.account.vo.MenuTreeVO;
import com.fixguild.common.domain.CommonResponse;
import com.fixguild.common.enums.ClientCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 租户端菜单管理接口。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@RestController
@RequestMapping("/tenant/api/menu")
@RequiredArgsConstructor
@Tag(name = "租户端-菜单管理")
public class MenuController {

    /** 菜单查询服务。 */
    private final MenuService menuService;

    /**
     * 查询租户端完整菜单树，包含按钮节点。
     *
     * @return 菜单树
     */
    @PostMapping("/tree")
    @Operation(summary = "菜单树", description = "返回租户端完整菜单树，包含按钮节点")
    @SaCheckPermission("tenant:menu:tree")
    public CommonResponse<List<MenuTreeVO>> tree() {
        return CommonResponse.ok(menuService.tree(ClientCode.TENANT.getCode()));
    }
}
