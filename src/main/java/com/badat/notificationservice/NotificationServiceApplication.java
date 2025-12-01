package com.badat.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.badat.notificationservice.config.JacksonConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Notification Service Application
 * Multi-channel notification service with Redis integration
 *
 * Features:
 * - Multi-channel support (EMAIL, SMS, PUSH, WEBHOOK, IN_APP)
 * - @Scheduled worker for processing
 * - REST API endpoints for queue management
 * - Automatic retry logic and failure handling
 * - Database persistence with full audit trail
 */
@SpringBootApplication
@EnableScheduling
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

}
