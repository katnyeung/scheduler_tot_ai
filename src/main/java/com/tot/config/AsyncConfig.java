package com.tot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous execution and scheduling
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Configure the executor used for @Async methods
     * This creates a thread pool for handling concurrent schedule processing
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Set the core pool size - the number of threads to keep alive even when idle
        executor.setCorePoolSize(5);

        // Maximum pool size - the maximum number of threads to allow in the pool
        executor.setMaxPoolSize(10);

        // Queue capacity - how many tasks can wait if all threads are busy
        executor.setQueueCapacity(25);

        // Thread name prefix - helps with debugging
        executor.setThreadNamePrefix("tot-async-");

        // Wait for tasks to finish on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Rejection policy: what to do when both the queue and pool are full
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // Initialize the executor
        executor.initialize();

        return executor;
    }
}