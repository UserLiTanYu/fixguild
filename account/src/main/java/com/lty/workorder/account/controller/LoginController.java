package com.lty.workorder.account.controller;

import com.lty.workorder.account.param.PlatformLoginParam;
import com.lty.workorder.account.param.TenantLoginParam;
import com.lty.workorder.account.service.LoginService;
import com.lty.workorder.account.vo.LoginVO;
import com.lty.workorder.common.domain.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录接口。
 *
 * @author litanyu
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/account/api")
@RequiredArgsConstructor
@Tag(name = "账号权限-登录")
public class LoginController {

    /** 登录服务。 */
    private final LoginService loginService;

    /**
     * 平台端登录。
     *
     * @param param 请求参数
     * @return 统一响应
     */
    @PostMapping("/platform/login")
    @Operation(summary = "平台端登录")
    public CommonResponse<LoginVO> platformLogin(@Valid @RequestBody PlatformLoginParam param) {
        return CommonResponse.ok(loginService.loginPlatform(param));
    }

    /**
     * 租户端登录。
     *
     * @param param 请求参数
     * @return 统一响应
     */
    @PostMapping("/tenant/login")
    @Operation(summary = "租户端登录")
    public CommonResponse<LoginVO> tenantLogin(@Valid @RequestBody TenantLoginParam param) {
        return CommonResponse.ok(loginService.loginTenant(param));
    }
}
