package com.colorinchi.app.config;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables asynchronous method execution and scheduled tasks for the
 * analytics pipeline.
 *
 * <p>The executor is tuned for light-weight, fire-and-forget analytics
 * writes. Core pool = 2 threads, max = 4, queue capacity = 512.</p>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean("analyticsTaskExecutor")
    public Executor analyticsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(512);
        executor.setThreadNamePrefix("analytics-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        log.info("analyticsTaskExecutor initialised: core={}, max={}", 2, 4);
        return executor;
    }
}
