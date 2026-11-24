package com.fixguild.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fixguild.account.param.SysRoleCreateParam;
import com.fixguild.account.param.SysRoleMenusParam;
import com.fixguild.account.param.SysRolePageParam;
import com.fixguild.account.param.SysRoleUpdateParam;
import com.fixguild.account.service.SysRoleService;
import com.fixguild.account.vo.SysRoleVO;
import com.fixguild.common.domain.CommonResponse;
import com.fixguild.common.domain.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户端角色管理接口。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@RestController
@RequestMapping("/tenant/api/role")
@RequiredArgsConstructor
@Tag(name = "租户端-角色管理")
public class RoleController {

    /** 角色管理服务。 */
    private final SysRoleService sysRoleService;

    /**
     * 角色分页。
     *
     * @param param 分页与筛选参数
     * @return 角色分页结果
     */
    @PostMapping("/page")
    @Operation(summary = "角色分页")
    @SaCheckPermission("tenant:role:page")
    public CommonResponse<PageResult<SysRoleVO>> page(@Valid @RequestBody SysRolePageParam param) {
        return CommonResponse.ok(sysRoleService.page(param));
    }

    /**
     * 新增角色。
     *
     * @param param 新增参数
     * @return 角色业务 ID
     */
    @PostMapping("/create")
    @Operation(summary = "新增角色")
    @SaCheckPermission("tenant:role:create")
    public CommonResponse<String> create(@Valid @RequestBody SysRoleCreateParam param) {
        return CommonResponse.ok(sysRoleService.create(param));
    }

    /**
     * 编辑角色。
     *
     * @param param 编辑参数
     * @return 统一响应
     */
    @PostMapping("/update")
    @Operation(summary = "编辑角色")
    @SaCheckPermission("tenant:role:update")
    public CommonResponse<Void> update(@Valid @RequestBody SysRoleUpdateParam param) {
        sysRoleService.update(param);
        return CommonResponse.ok();
    }

    /**
     * 分配角色菜单。
     *
     * @param param 角色菜单参数
     * @return 统一响应
     */
    @PostMapping("/menus")
    @Operation(summary = "分配角色菜单")
    @SaCheckPermission("tenant:role:menus")
    public CommonResponse<Void> menus(@Valid @RequestBody SysRoleMenusParam param) {
        sysRoleService.bindMenus(param);
        return CommonResponse.ok();
    }
}
