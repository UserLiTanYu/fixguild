package com.lty.workorder.common.filter;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 为每个请求生成 traceId，写入 MDC 和响应头。
 *
 * <p>优先复用上游传入的 X-Trace-Id，便于跨服务串联链路；没有时生成 UUID。
 * 最高优先级执行，保证后续日志都能带上 traceId；finally 清理 MDC，避免线程池复用串请求。</p>
 *
 * @author litanyu
 * @since 2026-11-19
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    /** MDC 中的 traceId key */
    public static final String TRACE_ID = "traceId";

    /** 响应头/上游透传的 traceId header */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 为当前请求准备 traceId，并保证请求结束后清理 MDC。
     *
     * @param request     当前请求
     * @param response    当前响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 复用上游 traceId，保证跨服务调用链完整
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
        }

        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 线程池复用线程，必须清理 MDC，避免 traceId 串请求
            MDC.remove(TRACE_ID);
        }
    }
}
