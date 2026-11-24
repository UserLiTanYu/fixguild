package com.fixguild.account.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fixguild.account.param.TenantUserCreateParam;
import com.fixguild.account.param.TenantUserPageParam;
import com.fixguild.account.param.TenantUserResetPasswordParam;
import com.fixguild.account.param.TenantUserStatusParam;
import com.fixguild.account.param.TenantUserUpdateParam;
import com.fixguild.account.service.TenantUserService;
import com.fixguild.account.vo.TenantUserCreateVO;
import com.fixguild.account.vo.TenantUserResetPasswordVO;
import com.fixguild.account.vo.TenantUserVO;
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
 * 租户端用户管理接口。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@RestController
@RequestMapping("/tenant/api/user")
@RequiredArgsConstructor
@Tag(name = "租户端-用户管理")
public class TenantUserController {

    /** 租户用户管理服务。 */
    private final TenantUserService tenantUserService;

    /**
     * 用户分页。
     *
     * @param param 分页与筛选参数
     * @return 用户分页结果
     */
    @PostMapping("/page")
    @Operation(summary = "用户分页")
    @SaCheckPermission("tenant:user:page")
    public CommonResponse<PageResult<TenantUserVO>> page(@Valid @RequestBody TenantUserPageParam param) {
        return CommonResponse.ok(tenantUserService.page(param));
    }

    /**
     * 新增用户。
     *
     * @param param 新增参数
     * @return 用户 ID、登录名和一次性初始密码
     */
    @PostMapping("/create")
    @Operation(summary = "新增用户", description = "初始密码仅返回一次")
    @SaCheckPermission("tenant:user:create")
    public CommonResponse<TenantUserCreateVO> create(@Valid @RequestBody TenantUserCreateParam param) {
        return CommonResponse.ok(tenantUserService.create(param));
    }

    /**
     * 编辑用户。
     *
     * @param param 编辑参数
     * @return 统一响应
     */
    @PostMapping("/update")
    @Operation(summary = "编辑用户")
    @SaCheckPermission("tenant:user:update")
    public CommonResponse<Void> update(@Valid @RequestBody TenantUserUpdateParam param) {
        tenantUserService.update(param);
        return CommonResponse.ok();
    }

    /**
     * 重置用户密码。
     *
     * @param param 重置密码参数
     * @return 用户 ID、登录名和一次性新密码
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置用户密码", description = "新密码仅返回一次，目标用户登录态立即失效")
    @SaCheckPermission("tenant:user:reset")
    public CommonResponse<TenantUserResetPasswordVO> resetPassword(
            @Valid @RequestBody TenantUserResetPasswordParam param) {
        return CommonResponse.ok(tenantUserService.resetPassword(param));
    }

    /**
     * 启用或停用用户。
     *
     * @param param 启停参数
     * @return 统一响应
     */
    @PostMapping("/status")
    @Operation(summary = "用户启停", description = "停用后目标用户登录态立即失效")
    @SaCheckPermission("tenant:user:status")
    public CommonResponse<Void> status(@Valid @RequestBody TenantUserStatusParam param) {
        tenantUserService.changeStatus(param);
        return CommonResponse.ok();
    }
}
