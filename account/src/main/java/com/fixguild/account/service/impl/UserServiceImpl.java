package com.fixguild.account.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.fixguild.account.entity.SysUserEntity;
import com.fixguild.account.support.AccountCredentialGenerator;
import com.fixguild.account.mapper.SysUserMapper;
import com.fixguild.account.param.PasswordChangeParam;
import com.fixguild.account.security.LoginAttemptGuard;
import com.fixguild.account.service.UserService;
import com.fixguild.common.auth.LoginUser;
import com.fixguild.common.enums.ClientCode;
import com.fixguild.common.enums.ErrorCode;
import com.fixguild.common.exception.BusinessException;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户账号服务实现。
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 用户表 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /** 登录失败锁定器。 */
    private final LoginAttemptGuard loginAttemptGuard;

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(LoginUser loginUser, PasswordChangeParam param) {
        SysUserEntity user = sysUserMapper.selectOneByQuery(QueryWrapper.create()
                .eq(SysUserEntity::getUserCode, loginUser.userCode())
                .eq(SysUserEntity::getTenantId, loginUser.tenantId())
                .eq(SysUserEntity::getClientCode, loginUser.clientCode())
                .eq(SysUserEntity::getStatus, 1));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已停用");
        }
        if (!passwordMatches(param.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "旧密码错误");
        }

        SysUserEntity update = new SysUserEntity();
        update.setId(user.getId());
        update.setPassword(AccountCredentialGenerator.hashPassword(param.getNewPassword()));
        sysUserMapper.update(update, true);

        // 密码已重置，清除该账号的登录失败记录，避免旧锁定状态影响重新登录
        ClientCode clientCode = ClientCode.fromCode(loginUser.clientCode());
        if (clientCode != null) {
            loginAttemptGuard.clear(clientCode, loginUser.tenantId(), user.getUsername());
        }
        log.info("修改密码成功: client={}, tenantId={}, userCode={}",
                loginUser.clientCode(), loginUser.tenantId(), loginUser.userCode());
    }

    /**
     * 校验原始密码与 BCrypt 哈希是否匹配。
     *
     * @param rawPassword     原始密码
     * @param encodedPassword BCrypt 哈希
     * @return 是否匹配
     */
    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        if (StrUtil.isBlank(encodedPassword)) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, encodedPassword);
        } catch (IllegalArgumentException e) {
            log.warn("密码哈希格式不正确");
            return false;
        }
    }
}
