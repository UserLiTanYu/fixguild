package com.lty.workorder.common.tenant;

/**
 * 当前线程的租户上下文。
 *
 * <p>HTTP 请求由拦截器写入（M2 接入登录态后）；MQ 消费、定时任务、异步线程必须显式 set/clear。
 * ThreadLocal 不跨线程传递，线程池复用前未清理会导致租户串数据。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
public final class TenantContext {

    /** 当前线程的租户编码 */
    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    /** 是否忽略租户隔离；仅平台端全局操作使用 */
    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> false);

    /**
     * 工具类禁止实例化。
     */
    private TenantContext() {
    }

    /**
     * 设置当前线程的租户编码。
     *
     * @param tenantId 租户编码
     */
    public static void set(String tenantId) {
        TENANT.set(tenantId);
    }

    /**
     * 获取当前线程的租户编码。
     *
     * @return 租户编码，未设置时为 null
     */
    public static String get() {
        return TENANT.get();
    }

    /**
     * 判断当前线程是否忽略租户隔离。
     *
     * @return 忽略返回 true
     */
    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /**
     * 标记当前线程忽略租户隔离。
     *
     * <p>仅用于平台端全局表操作；必须与 {@link #clear()} 配对，建议放在 finally 中。</p>
     */
    public static void ignore() {
        IGNORE.set(true);
    }


    public static void setIgnore(boolean ignore) {
        IGNORE.set(ignore);
    }

    /**
     * 清理当前线程的租户上下文，避免线程池复用时串数据。
     */
    public static void clear() {
        TENANT.remove();
        IGNORE.remove();
    }
}
