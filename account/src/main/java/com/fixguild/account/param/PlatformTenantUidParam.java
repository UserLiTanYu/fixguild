package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 仅包含租户业务 ID 的通用参数。
 *
 * <p>用于详情、编辑、启停等按业务 ID 操作的接口。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@Schema(description = "租户ID参数")
public class PlatformTenantUidParam {

    /** 租户业务 ID，最长 32 */
    @NotBlank(message = "uid 不能为空")
    @Size(max = 32, message = "uid 长度不能超过 32")
    @Schema(description = "租户业务ID")
    private String uid;
}
