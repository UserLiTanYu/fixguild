package com.lty.workorder.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页请求基类。
 *
 * <p>页码最小为 1，每页条数限制在 1~200；限制在 getter 中完成，
 * 调用方即使传入非法值也会被纠正。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Data
public class PageParam implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 页码，从 1 开始 */
    @Schema(description = "页码，从 1 开始", example = "1")
    private int pageNum = 1;

    /** 每页条数，最大 200 */
    @Schema(description = "每页条数，最大 200", example = "20")
    private int pageSize = 20;

    /**
     * 获取页码，小于 1 时按 1 处理。
     *
     * @return 有效页码
     */
    public int getPageNum() {
        return Math.max(pageNum, 1);
    }

    /**
     * 获取每页条数，超出 1~200 时自动收敛。
     *
     * @return 有效每页条数
     */
    public int getPageSize() {
        return Math.clamp(pageSize, 1, 200);
    }
}
