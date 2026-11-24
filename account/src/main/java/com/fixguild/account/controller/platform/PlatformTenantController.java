package com.fixguild.account.controller.platform;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fixguild.account.param.PlatformTenantCreateParam;
import com.fixguild.account.param.PlatformTenantPageParam;
import com.fixguild.account.param.PlatformTenantStatusParam;
import com.fixguild.account.param.PlatformTenantUidParam;
import com.fixguild.account.param.PlatformTenantUpdateParam;
import com.fixguild.account.service.platform.PlatformTenantService;
import com.fixguild.account.vo.PlatformTenantCreateVO;
import com.fixguild.account.vo.PlatformTenantVO;
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
 * 平台端租户管理接口。
 *
 * <p>租户表为平台全局表，不参与租户数据隔离；接口统一使用
 * {@code @SaCheckPermission} 校验平台端权限。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/platform/api/tenant")
@RequiredArgsConstructor
@Tag(name = "平台端-租户管理")
public class PlatformTenantController {

    /** 租户管理服务 */
    private final PlatformTenantService platformTenantService;

    /**
     * 分页查询租户。
     *
     * @param param 分页与筛选参数
     * @return 统一响应，data 为租户分页结果
     */
    @PostMapping("/page")
    @Operation(summary = "租户分页")
    @SaCheckPermission("platform:tenant:page")
    public CommonResponse<PageResult<PlatformTenantVO>> page(@Valid @RequestBody PlatformTenantPageParam param) {
        return CommonResponse.ok(platformTenantService.page(param));
    }

    /**
     * 新增租户，并同步创建租户端初始管理员。
     *
     * @param param 新增参数
     * @return 统一响应，data 包含租户 ID、管理员账号和一次性初始密码
     */
    @PostMapping("/create")
    @Operation(summary = "新增租户", description = "同步创建租户端初始管理员，初始密码仅返回一次")
    @SaCheckPermission("platform:tenant:create")
    public CommonResponse<PlatformTenantCreateVO> create(@Valid @RequestBody PlatformTenantCreateParam param) {
        return CommonResponse.ok(platformTenantService.create(param));
    }

    /**
     * 编辑租户。
     *
     * @param param 编辑参数
     * @return 统一响应，无业务数据
     */
    @PostMapping("/update")
    @Operation(summary = "编辑租户")
    @SaCheckPermission("platform:tenant:update")
    public CommonResponse<Void> update(@Valid @RequestBody PlatformTenantUpdateParam param) {
        platformTenantService.update(param);
        return CommonResponse.ok();
    }

    /**
     * 查询租户详情。
     *
     * @param param 租户 ID 参数
     * @return 统一响应，data 为租户详情
     */
    @PostMapping("/detail")
    @Operation(summary = "租户详情")
    @SaCheckPermission("platform:tenant:detail")
    public CommonResponse<PlatformTenantVO> detail(@Valid @RequestBody PlatformTenantUidParam param) {
        return CommonResponse.ok(platformTenantService.detail(param.getUid()));
    }

    /**
     * 启用或停用租户。
     *
     * @param param 租户 ID 与目标状态
     * @return 统一响应，无业务数据
     */
    @PostMapping("/status")
    @Operation(summary = "启停租户")
    @SaCheckPermission("platform:tenant:status")
    public CommonResponse<Void> status(@Valid @RequestBody PlatformTenantStatusParam param) {
        platformTenantService.changeStatus(param.getUid(), param.getStatus());
        return CommonResponse.ok();
    }
}
