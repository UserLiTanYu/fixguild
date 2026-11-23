package com.fixguild.account.security;

import com.fixguild.common.enums.ClientCode;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败锁定。
 *
 * <p>M2 使用本地内存；多实例部署时替换为 Redis 的 login:fail:{client}:{username}。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Component
public class LoginAttemptGuard {

    /** 最大登录失败次数。 */
    private static final int MAX_FAIL_COUNT = 5;

    /** 登录锁定时长。 */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /** 登录失败记录。 */
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * 判断账号是否处于登录锁定期。
     *
     * @param clientCode 登录端编码
     * @param tenantId 租户编码
     * @param username 登录名
     * @return 是否处于锁定期
     */
    public boolean isLocked(ClientCode clientCode, String tenantId, String username) {
        String key = key(clientCode, tenantId, username);
        Attempt attempt = attempts.get(key);
        if (attempt == null) {
            return false;
        }
        if (attempt.lockedUntil != null) {
            if (attempt.lockedUntil.isAfter(Instant.now())) {
                return true;
            }
            attempts.remove(key);
        }
        return false;
    }

    /**
     * 记录一次登录失败，达到阈值后锁定。
     *
     * @param clientCode 登录端编码
     * @param tenantId 租户编码
     * @param username 登录名
     */
    public void recordFailure(ClientCode clientCode, String tenantId, String username) {
        String key = key(clientCode, tenantId, username);
        attempts.compute(key, (k, old) -> {
            Attempt attempt = old == null ? new Attempt() : old;
            attempt.failCount++;
            if (attempt.failCount >= MAX_FAIL_COUNT) {
                attempt.lockedUntil = Instant.now().plus(LOCK_DURATION);
            }
            return attempt;
        });
    }

    /**
     * 清除登录失败记录。
     *
     * @param clientCode 登录端编码
     * @param tenantId 租户编码
     * @param username 登录名
     */
    public void clear(ClientCode clientCode, String tenantId, String username) {
        attempts.remove(key(clientCode, tenantId, username));
    }

    /** 生成登录失败记录键。 */
    private String key(ClientCode clientCode, String tenantId, String username) {
        return clientCode.getCode() + ":" + tenantId + ":" + username;
    }

    /**
     * 单个账号的登录失败记录。
     */
    private static final class Attempt {

        /** 连续失败次数。 */
        private int failCount;

        /** 锁定截止时间，null 表示未锁定。 */
        private Instant lockedUntil;
    }
}
