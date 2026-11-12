package com.pet.vetalya.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "app.logging.http")
public record LoggingProperties(
        long slowRequestThresholdMs,
        List<String> bodyLoggingPaths,
        int maxBodySize
) {
    public LoggingProperties {
        if (slowRequestThresholdMs <= 0) {
            slowRequestThresholdMs = 1000L;
        }
        if (bodyLoggingPaths == null) {
            bodyLoggingPaths = Collections.emptyList();
        }
        if (maxBodySize <= 0) {
            maxBodySize = 1024;
        }
    }
}

