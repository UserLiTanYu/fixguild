package com.fixguild.account.controller.platform;

import com.fixguild.account.dto.LoginClientInfo;
import com.fixguild.account.param.PlatformLoginParam;
import com.fixguild.account.service.auth.LoginService;
import com.fixguild.account.vo.LoginVO;
import com.fixguild.common.domain.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端登录接口。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@RestController
@RequestMapping("/account/api")
@RequiredArgsConstructor
@Tag(name = "平台端-登录")
public class PlatformLoginController {

    /** 登录服务。 */
    private final LoginService loginService;

    /**
     * 平台端登录。
     *
     * @param param   请求参数
     * @param request 当前 HTTP 请求，用于记录登录 IP 和 User-Agent
     * @return 统一响应
     */
    @PostMapping("/platform/login")
    @Operation(summary = "平台端登录")
    public CommonResponse<LoginVO> login(@Valid @RequestBody PlatformLoginParam param,
                                         HttpServletRequest request) {
        return CommonResponse.ok(loginService.loginPlatform(param, LoginClientInfo.from(request)));
    }
}
