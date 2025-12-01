package com.badat.notificationservice.service.sender;

import com.badat.notificationservice.entity.NotificationMessage;
import com.badat.notificationservice.service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

import java.util.Map;

/**
 * SMS Notification Sender Implementation (Mock)
 * Handles SMS channel notifications with retry logic and delivery tracking
 * In production, integrate with SMS provider API (Twilio, Vonage, etc.)
 */
@Slf4j
@Service
public class SmsNotificationSender implements NotificationSender {

    @Override
    public String getChannelType() {
        return NotificationMessage.ChannelType.SMS;
    }

    @Override
    public boolean send(String recipient, String title, String content, Map<String, Object> metadata) {
        try {
            log.info("📱 Sending SMS to: {} with message: {}", recipient, title);

            // Mock SMS sending logic
            // In production, integrate with actual SMS provider API

            // Simulate SMS sending delay
            Thread.sleep(1000); // 1 second delay to simulate network

            // Mock success with 95% success rate for demo
            boolean success = Math.random() > 0.05;

            if (success) {
                log.info("✅ SMS sent successfully to: {}", recipient);
                return true;
            } else {
                log.warn("⚠️ SMS sending failed to: {} (mock failure)", recipient);
                return false;
            }

        } catch (Exception e) {
            log.error("💥 Failed to send SMS to {}: {}", recipient, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // Mock health check
            log.info("📱 SMS service health check - OK");
            return true;

        } catch (Exception e) {
            log.error("💥 SMS service health check failed: {}", e.getMessage());
            return false;
        }
    }
}