package com.lty.workorder.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：所有业务表统一字段。
 *
 * <p>统一提供主键、审计字段、备注和逻辑删除标记；带租户字段的业务表继承
 * {@link TenantEntity}，平台级全局表直接继承本类。时间字段统一按 GMT+8 输出。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键，数据库自增 */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 创建人，插入时由 AuditFieldListener 填充 */
    @Column(value = "create_by")
    private String createBy;

    /** 更新人，插入/更新时由 AuditFieldListener 填充 */
    @Column(value = "update_by")
    private String updateBy;

    /** 创建时间，插入时填充，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(value = "create_time", onInsertValue = "now()")
    private LocalDateTime createTime;

    /** 更新时间，插入/更新时填充，格式 yyyy-MM-dd HH:mm:ss */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Column(value = "update_time", onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /** 备注 */
    private String remark;

    /** 逻辑删除标记：0 未删除，1 已删除；查询由 MyBatis-Flex 自动过滤 */
    @Column(value = "is_delete", isLogicDelete = true)
    private Integer isDelete;
}
