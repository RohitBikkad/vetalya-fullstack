package com.pet.vetalya.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.logging.MdcTaskDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated thread pool for non-blocking asynchronous audit log persistence.
 * If the bounded queue is saturated, tasks are discarded and a metric counter
 * is incremented,
 * ensuring business request execution is never slowed down or aborted.
 */
@Configuration
public class AuditAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AuditAsyncConfig.class);

    @Value("${app.audit.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${app.audit.core-pool-size:2}")
    private int corePoolSize;

    @Value("${app.audit.max-pool-size:4}")
    private int maxPoolSize;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean(name = "auditExecutor")
    public Executor auditExecutor(io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("audit-exec-");
        executor.setTaskDecorator(new MdcTaskDecorator());

        // Custom rejection policy: drop task and record dropped metric without throwing
        executor.setRejectedExecutionHandler((runnable, exec) -> {
            log.warn("Audit log task queue is full (capacity={}). Discarding audit event.", queueCapacity);
            meterRegistry.counter("audit.write.failed", "reason", "queue_full").increment();
        });

        executor.initialize();
        return executor;
    }
}
