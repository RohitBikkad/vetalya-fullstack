package com.pet.vetalya.logging;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.UUID;

/**
 * Propagates the MDC context map from caller threads to @Async background task threads,
 * and clears the context after execution to avoid thread-pool pollution.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                } else {
                    String correlationId = UUID.randomUUID().toString();
                    MDC.put(CorrelationIdFilter.MDC_CORRELATION_ID_KEY, correlationId);
                    MDC.put(CorrelationIdFilter.MDC_TRACE_ID_KEY, correlationId);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

