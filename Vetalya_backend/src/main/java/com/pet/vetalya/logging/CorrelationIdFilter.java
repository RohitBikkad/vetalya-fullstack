package com.pet.vetalya.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Highest-precedence servlet filter managing correlation IDs and preventing log injection.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_ID_HEADER = "X-Request-Id";
    public static final String MDC_CORRELATION_ID_KEY = "correlationId";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    // Strictly alphanumeric, hyphens, and underscores up to 64 characters to prevent CRLF log injection
    private static final Pattern VALID_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest httpRequest)
                || !(servletResponse instanceof HttpServletResponse httpResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String rawHeader = httpRequest.getHeader(CORRELATION_ID_HEADER);
        String correlationId;
        if (rawHeader != null && VALID_CORRELATION_ID_PATTERN.matcher(rawHeader.trim()).matches()) {
            correlationId = rawHeader.trim();
        } else {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_ID_KEY, correlationId);
        MDC.put(MDC_TRACE_ID_KEY, correlationId);
        httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove(MDC_CORRELATION_ID_KEY);
            MDC.remove(MDC_TRACE_ID_KEY);
            MDC.remove("userId");
            MDC.remove("role");
            MDC.remove("jobRunId");
        }
    }

    public static String getCorrelationId() {
        String id = MDC.get(MDC_CORRELATION_ID_KEY);
        return id != null ? id : UUID.randomUUID().toString();
    }
}

