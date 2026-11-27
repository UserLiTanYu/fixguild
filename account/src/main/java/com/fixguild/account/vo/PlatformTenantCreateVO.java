package com.fixguild.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增租户结果。
 *
 * <p>平台创建租户时会同步创建初始管理员。初始密码只在本次响应中返回一次，
 * 数据库仅保存 BCrypt 哈希，调用方需要妥善保存并及时交付给租户管理员。</p>
 *
 * @author litanyu
 * @since 2026-11-24
 */
@Data
@Schema(description = "新增租户结果")
public class PlatformTenantCreateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 租户业务 ID。 */
    @Schema(description = "租户业务 ID")
    private String uid;

    /** 租户编码。 */
    @Schema(description = "租户编码", example = "T1001")
    private String tenantCode;

    /** 初始管理员登录名。 */
    @Schema(description = "初始管理员登录名", example = "tenant_admin")
    private String adminUsername;

    /** 初始管理员业务 ID。 */
    @Schema(description = "初始管理员业务 ID")
    private String adminUserUid;

    /** 初始管理员密码明文，仅本次返回。 */
    @Schema(description = "初始管理员密码，仅本次返回，请妥善保存")
    private String initialPassword;
}
