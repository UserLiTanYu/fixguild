package com.fixguild.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略 MyBatis-Flex 租户过滤。
 *
 * <p>仅允许平台端跨租户查询使用；使用前必须校验平台权限，并在查询中显式补 tenant_id 条件。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreTenant {
}
