package com.fixguild.account.support;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;

/**
 * 账号凭据生成工具。
 *
 * <p>用户编码用于 Sa-Token loginId，必须全局唯一；初始密码只在创建或重置时返回一次，
 * 数据库只保存 BCrypt 哈希。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Slf4j
public final class AccountCredentialGenerator {

    /** 用户编码前缀。 */
    private static final String USER_CODE_PREFIX = "U";

    /** 初始密码固定前缀。 */
    private static final String INITIAL_PASSWORD_PREFIX = "Fg@";

    /** 初始密码随机部分长度。 */
    private static final int PASSWORD_RANDOM_LENGTH = 12;

    /** BCrypt 计算强度。 */
    private static final int BCRYPT_STRENGTH = 10;

    private AccountCredentialGenerator() {
    }

    /**
     * 生成全局唯一的用户编码。
     *
     * @return 用户编码
     */
    public static String generateUserCode() {
        return USER_CODE_PREFIX + IdUtil.fastSimpleUUID().substring(0, 16).toUpperCase();
    }

    /**
     * 生成一次性初始密码。
     *
     * @return 初始密码明文
     */
    public static String generateInitialPassword() {
        return INITIAL_PASSWORD_PREFIX
                + RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, PASSWORD_RANDOM_LENGTH);
    }

    /**
     * 生成密码哈希。
     *
     * @param rawPassword 原始密码
     * @return BCrypt 哈希
     */
    public static String hashPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_STRENGTH));
    }

    /**
     * 校验原始密码与 BCrypt 哈希是否匹配。
     *
     * @param rawPassword     原始密码
     * @param encodedPassword BCrypt 哈希
     * @return 是否匹配
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
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
