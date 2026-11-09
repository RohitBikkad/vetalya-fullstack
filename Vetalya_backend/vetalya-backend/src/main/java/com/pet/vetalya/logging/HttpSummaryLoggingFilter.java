package com.pet.vetalya.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Emits one structured summary line per HTTP request and records slow request warnings.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpSummaryLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpSummaryLoggingFilter.class);
    private static final Marker SLOW_REQUEST_MARKER = MarkerFactory.getMarker("slow_request");

    private final LoggingProperties properties;

    @org.springframework.beans.factory.annotation.Autowired
    public HttpSummaryLoggingFilter(@org.springframework.beans.factory.annotation.Autowired(required = false) LoggingProperties properties) {
        this.properties = properties != null ? properties : new LoggingProperties(1000L, java.util.Collections.emptyList(), 1024);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        boolean isHealthOrMetrics = uri.startsWith("/actuator/health") || uri.startsWith("/actuator/prometheus");

        boolean enableBodyLogging = isBodyLoggingEnabled(uri);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, properties.maxBodySize());
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int status = responseWrapper.getStatus();
            byte[] responseBytes = responseWrapper.getContentAsByteArray();
            int responseSize = responseBytes.length;

            String pathTemplate = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (pathTemplate == null || pathTemplate.isBlank()) {
                pathTemplate = uri;
            }

            String clientType = request.getHeader("X-Client-Type");
            if (clientType == null || clientType.isBlank()) {
                clientType = "unknown";
            }
            String appVersion = request.getHeader("X-App-Version");
            String versionInfo = (appVersion != null && !appVersion.isBlank()) ? " v=" + appVersion : "";

            boolean isSlow = durationMs >= properties.slowRequestThresholdMs();

            String summaryMessage = String.format(
                    "HTTP %s %s -> status=%d duration=%dms size=%dB client=%s%s%s",
                    request.getMethod(),
                    pathTemplate,
                    status,
                    durationMs,
                    responseSize,
                    clientType,
                    versionInfo,
                    isSlow ? " [slow_request]" : ""
            );

            if (enableBodyLogging) {
                summaryMessage += formatBodyDetails(requestWrapper, responseBytes);
            }

            logSummary(summaryMessage, status, isSlow, isHealthOrMetrics);

            // Crucial: copy content to the original response output stream
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logSummary(String message, int status, boolean isSlow, boolean isHealthOrMetrics) {
        if (isHealthOrMetrics) {
            log.debug("{}", message);
            return;
        }

        if (status >= 500) {
            if (isSlow) {
                log.error(SLOW_REQUEST_MARKER, "{}", message);
            } else {
                log.error("{}", message);
            }
        } else if (status >= 400 || isSlow) {
            if (isSlow) {
                log.warn(SLOW_REQUEST_MARKER, "{}", message);
            } else {
                log.warn("{}", message);
            }
        } else {
            log.info("{}", message);
        }
    }

    private boolean isBodyLoggingEnabled(String uri) {
        if (uri.startsWith("/api/auth") || uri.startsWith("/api/public/auth")) {
            return false; // Never log bodies on authentication endpoints
        }
        for (String pattern : properties.bodyLoggingPaths()) {
            if (uri.startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String formatBodyDetails(ContentCachingRequestWrapper req, byte[] respBytes) {
        StringBuilder sb = new StringBuilder();
        byte[] reqBytes = req.getContentAsByteArray();
        if (reqBytes.length > 0) {
            int len = Math.min(reqBytes.length, properties.maxBodySize());
            String reqBody = new String(reqBytes, 0, len, StandardCharsets.UTF_8);
            sb.append(" | req_body=").append(SensitiveDataMaskingConverter.mask(reqBody));
        }
        if (respBytes.length > 0) {
            int len = Math.min(respBytes.length, properties.maxBodySize());
            String respBody = new String(respBytes, 0, len, StandardCharsets.UTF_8);
            sb.append(" | resp_body=").append(SensitiveDataMaskingConverter.mask(respBody));
        }
        return sb.toString();
    }
}
