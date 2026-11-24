package com.fixguild.account.controller;

import com.fixguild.account.service.AuthService;
import com.fixguild.account.vo.UserAuthVO;
import com.fixguild.account.vo.UserInfoVO;
import com.fixguild.common.auth.LoginContext;
import com.fixguild.common.auth.LoginUser;
import com.fixguild.common.domain.CommonResponse;
import com.fixguild.common.enums.ClientCode;
import com.fixguild.common.enums.ErrorCode;
import com.fixguild.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户接口。
 *
 * <p>路径中的 client 必须与 token 的登录端一致，避免登录端串用。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@RestController
@RequestMapping("/account/api")
@RequiredArgsConstructor
@Tag(name = "账号权限-当前用户")
public class UserController {

    /** 权限查询服务。 */
    private final AuthService authService;

    /**
     * 查询当前用户信息。
     *
     * @param client 登录端编码
     * @return 当前用户信息
     */
    @PostMapping("/{client}/user/info")
    @Operation(summary = "当前用户信息")
    public CommonResponse<UserInfoVO> info(@PathVariable String client) {
        LoginUser loginUser = currentLogin(client);
        return CommonResponse.ok(authService.getUserInfo(loginUser));
    }

    /**
     * 查询当前用户的角色、权限和菜单树。
     *
     * @param client 登录端编码
     * @return 权限信息
     */
    @PostMapping("/{client}/user/auth")
    @Operation(summary = "当前用户权限菜单")
    public CommonResponse<UserAuthVO> auth(@PathVariable String client) {
        LoginUser loginUser = currentLogin(client);
        return CommonResponse.ok(authService.getAuth(loginUser));
    }

    /**
     * 校验路径登录端与 token 登录端一致。
     *
     * @param client 路径中的登录端
     * @return 当前登录用户
     * @throws BusinessException 登录端不合法、未登录或登录端不匹配
     */
    private LoginUser currentLogin(String client) {
        ClientCode requested = ClientCode.fromCode(client);
        if (requested == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录端不合法");
        }
        LoginUser loginUser = LoginContext.get();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!requested.getCode().equals(loginUser.clientCode())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录端不匹配");
        }
        return loginUser;
    }
}
