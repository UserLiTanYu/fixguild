package com.fixguild.account.controller.tenant;

import com.fixguild.account.param.TenantLoginParam;
import com.fixguild.account.service.auth.LoginService;
import com.fixguild.account.vo.LoginVO;
import com.fixguild.common.domain.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户端登录接口。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@RestController
@RequestMapping("/account/api")
@RequiredArgsConstructor
@Tag(name = "账号权限-租户登录")
public class TenantLoginController {

    /** 登录服务。 */
    private final LoginService loginService;

    /**
     * 租户端登录。
     *
     * @param param 请求参数
     * @return 统一响应
     */
    @PostMapping("/tenant/login")
    @Operation(summary = "租户端登录")
    public CommonResponse<LoginVO> login(@Valid @RequestBody TenantLoginParam param) {
        return CommonResponse.ok(loginService.loginTenant(param));
    }
}
