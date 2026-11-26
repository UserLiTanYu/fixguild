package com.fixguild.account.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 租户端按业务 ID 操作的通用参数。
 *
 * @author litanyu
 * @since 2026-11-26
 */
@Data
@Schema(description = "租户端业务 ID 参数")
public class TenantUidParam {

    /** 业务 ID。 */
    @NotBlank(message = "ID 不能为空")
    @Size(max = 32, message = "ID 长度不能超过 32")
    @Schema(description = "业务 ID")
    private String uid;
}
