package com.fixguild.common.config;

import com.fixguild.common.listener.AuditFieldListener;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 全局配置。
 *
 * <p>构造阶段开启 SQL 审计并设置慢 SQL 阈值；customize 阶段注册数据库方言和
 * 审计字段监听器。审计开关是进程级副作用，单元测试中需要注意执行顺序。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Slf4j
@Configuration
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    /** 审计字段监听器，注册到 MyBatis-Flex 全局配置 */
    private final AuditFieldListener auditFieldListener;

    /**
     * 创建配置并开启 SQL 审计。
     *
     * @param auditFieldListener 审计字段监听器
     */
    public MyBatisFlexConfig(AuditFieldListener auditFieldListener) {
        this.auditFieldListener = auditFieldListener;
        // 开启 SQL 审计，便于本地排查慢查询和异常 SQL
        AuditManager.setAuditEnable(true);

        // 500ms 以上记为慢 SQL；正常 SQL 降为 debug，避免日志被淹没
        AuditManager.setMessageCollector(message -> {
            long elapsed = message.getElapsedTime();
            if (elapsed >= 500) {
                log.warn("慢 SQL: {} ({}ms)", message.getQuery(), elapsed);
            } else {
                log.debug("SQL: {} ({}ms)", message.getQuery(), elapsed);
            }
        });
    }

    /**
     * 注册数据库方言和实体监听器。
     *
     * @param globalConfig MyBatis-Flex 全局配置
     */
    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // H2 以 MySQL 模式运行，统一按 MySQL 方言生成 SQL
        globalConfig.setDbType(DbType.MYSQL);
        // 插入/更新时自动填充审计字段
        globalConfig.registerInsertListener(auditFieldListener);
        globalConfig.registerUpdateListener(auditFieldListener);
        log.info("MyBatis-Flex 全局监听器注册完成");
    }
}
