package com.lty.workorder.common.domain;

import com.mybatisflex.core.paginate.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应，避免直接暴露 MyBatis-Flex Page 的字段命名。
 *
 * <p>对外只暴露 records/total/pageNum/pageSize，分页实现变化不影响 API 契约。</p>
 *
 * @param <T> 记录类型
 * @author litanyu
 * @since 2026-11-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前页记录 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private long pageNum;

    /** 每页条数 */
    private long pageSize;

    /**
     * 将 MyBatis-Flex 分页对象转换为统一响应结构。
     *
     * @param page MyBatis-Flex 分页结果
     * @param <T>  记录类型
     * @return 统一分页响应
     */
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotalRow(),
                page.getPageNumber(),
                page.getPageSize()
        );
    }
}
