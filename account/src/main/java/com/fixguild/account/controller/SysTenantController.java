package com.fixguild.account.controller;

import com.fixguild.account.param.SysTenantCreateParam;
import com.fixguild.account.param.SysTenantPageParam;
import com.fixguild.account.param.SysTenantStatusParam;
import com.fixguild.account.param.SysTenantUidParam;
import com.fixguild.account.param.SysTenantUpdateParam;
import com.fixguild.account.service.SysTenantService;
import com.fixguild.account.vo.SysTenantVO;
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
 * <p>租户表为平台全局表，不参与租户数据隔离；M2 接入 Sa-Token 后补
 * {@code @SaCheckPermission} 权限校验。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/platform/api/tenant")
@RequiredArgsConstructor
@Tag(name = "平台端-租户管理")
public class SysTenantController {

    /** 租户管理服务 */
    private final SysTenantService sysTenantService;

    /**
     * 分页查询租户。
     *
     * @param param 分页与筛选参数
     * @return 统一响应，data 为租户分页结果
     */
    @PostMapping("/page")
    @Operation(summary = "租户分页")
    public CommonResponse<PageResult<SysTenantVO>> page(@Valid @RequestBody SysTenantPageParam param) {
        return CommonResponse.ok(sysTenantService.page(param));
    }

    /**
     * 新增租户。
     *
     * @param param 新增参数
     * @return 统一响应，data 为租户业务 ID
     */
    @PostMapping("/create")
    @Operation(summary = "新增租户")
    public CommonResponse<String> create(@Valid @RequestBody SysTenantCreateParam param) {
        return CommonResponse.ok(sysTenantService.create(param));
    }

    /**
     * 编辑租户。
     *
     * @param param 编辑参数
     * @return 统一响应，无业务数据
     */
    @PostMapping("/update")
    @Operation(summary = "编辑租户")
    public CommonResponse<Void> update(@Valid @RequestBody SysTenantUpdateParam param) {
        sysTenantService.update(param);
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
    public CommonResponse<SysTenantVO> detail(@Valid @RequestBody SysTenantUidParam param) {
        return CommonResponse.ok(sysTenantService.detail(param.getUid()));
    }

    /**
     * 启用或停用租户。
     *
     * @param param 租户 ID 与目标状态
     * @return 统一响应，无业务数据
     */
    @PostMapping("/status")
    @Operation(summary = "启停租户")
    public CommonResponse<Void> status(@Valid @RequestBody SysTenantStatusParam param) {
        sysTenantService.changeStatus(param.getUid(), param.getStatus());
        return CommonResponse.ok();
    }
}
