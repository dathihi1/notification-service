package com.badat.notificationservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Optimized Notification Service Configuration
 * Simplified thread pools for better resource management
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class NotificationConfig {

    /**
     * Main thread pool for notification processing
     * Optimized for balanced performance and resource usage
     */
    @Bean("notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core threads - always alive for steady load
        executor.setCorePoolSize(3);

        // Maximum threads - can scale up under peak load
        executor.setMaxPoolSize(10);

        // Queue capacity - buffer for burst traffic
        executor.setQueueCapacity(50);

        // Thread name prefix for easy identification in logs
        executor.setThreadNamePrefix("notification-processor-");

        // Allow core threads to timeout - helps with resource management
        executor.setAllowCoreThreadTimeOut(true);

        // Keep alive time for idle threads
        executor.setKeepAliveSeconds(60);

        // Rejected execution policy - caller runs when queue is full
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Await termination time
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("🚀 Notification executor initialized: {} core, {} max threads, queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Dedicated thread pool for email sending
     * Separate to prevent I/O operations from blocking other notifications
     */
    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("email-sender-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("📧 Email executor initialized: {} core, {} max threads, queue: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Simplified thread pool monitor for health checks
     */
    @Bean
    public ThreadPoolMonitor threadPoolMonitor() {
        return new ThreadPoolMonitor();
    }

    /**
     * Lightweight Thread Pool Monitor
     * Focused on essential metrics for production monitoring
     */
    public static class ThreadPoolMonitor {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ThreadPoolMonitor.class);

        /**
         * Log essential thread pool statistics
         */
        public void logPoolStats(String poolName, ThreadPoolTaskExecutor executor) {
            log.info("📊 {} - Active: {}/{}, Queue: {}/{}",
                poolName,
                executor.getActiveCount(), executor.getPoolSize(),
                executor.getThreadPoolExecutor().getQueue().size(), executor.getQueueCapacity());
        }

        /**
         * Check if thread pool is healthy
         */
        public boolean isPoolHealthy(ThreadPoolTaskExecutor executor) {
            double queueUsageRatio = (double) executor.getThreadPoolExecutor().getQueue().size() / executor.getQueueCapacity();
            double activeUsageRatio = (double) executor.getActiveCount() / executor.getMaxPoolSize();

            // Consider unhealthy if queue is > 80% full or threads are too busy
            return queueUsageRatio < 0.8 && activeUsageRatio < 0.9;
        }

        /**
         * Get simplified pool health status
         */
        public String getPoolHealthStatus(ThreadPoolTaskExecutor executor) {
            if (isPoolHealthy(executor)) {
                return "HEALTHY";
            } else {
                double queueUsageRatio = (double) executor.getThreadPoolExecutor().getQueue().size() / executor.getQueueCapacity();

                if (queueUsageRatio > 0.8) {
                    return "QUEUE_OVERLOAD";
                } else {
                    return "THREAD_OVERLOAD";
                }
            }
        }
    }
}