package com.fixguild.account.controller.common;

import cn.dev33.satoken.stp.StpUtil;
import com.fixguild.account.param.CurrentUserPasswordChangeParam;
import com.fixguild.account.service.auth.AuthService;
import com.fixguild.account.service.common.CurrentUserService;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户接口。
 *
 * <p>路径中的 client 必须与 token 的登录端一致，避免登录端串用。
 * 本控制器只要求登录，不要求业务权限标识（规范 13.2 例外）。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@RestController
@RequestMapping("/account/api")
@RequiredArgsConstructor
@Tag(name = "公共-当前用户")
public class CurrentUserController {

    /** 权限查询服务。 */
    private final AuthService authService;

    /** 用户账号服务。 */
    private final CurrentUserService currentUserService;

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
     * 退出当前登录。
     *
     * @param client 登录端编码
     * @return 统一响应
     */
    @PostMapping("/{client}/logout")
    @Operation(summary = "退出登录")
    public CommonResponse<Void> logout(@PathVariable String client) {
        currentLogin(client);
        StpUtil.logout();
        return CommonResponse.ok();
    }

    /**
     * 修改当前用户密码，成功后退出当前登录。
     *
     * @param client 登录端编码
     * @param param  修改密码参数
     * @return 统一响应
     */
    @PostMapping("/{client}/password/change")
    @Operation(summary = "修改密码", description = "校验旧密码，修改成功后退出当前登录")
    public CommonResponse<Void> changePassword(@PathVariable String client,
                                               @Valid @RequestBody CurrentUserPasswordChangeParam param) {
        LoginUser loginUser = currentLogin(client);
        currentUserService.changePassword(loginUser, param);
        StpUtil.logout();
        return CommonResponse.ok();
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
