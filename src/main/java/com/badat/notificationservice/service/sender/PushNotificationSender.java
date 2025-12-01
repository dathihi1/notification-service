package com.badat.notificationservice.service.sender;

import com.badat.notificationservice.entity.NotificationMessage;
import com.badat.notificationservice.service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Push Notification Sender Implementation (Mock)
 * Handles PUSH_NOTIFICATION channel for mobile devices
 * In production, integrate with FCM (Firebase), APNs (Apple), or other push services
 */
@Slf4j
@Service
public class PushNotificationSender implements NotificationSender {

    @Override
    public String getChannelType() {
        return NotificationMessage.ChannelType.PUSH_NOTIFICATION;
    }

    @Override
    public boolean send(String recipient, String title, String content, Map<String, Object> metadata) {
        try {
            log.info("📱 Sending push notification to device: {} with title: {}", recipient, title);

            // Mock push sending logic
            // In production, integrate with actual push service (FCM, APNs)

            // Simulate device token validation
            if (!isValidDeviceToken(recipient)) {
                log.warn("⚠️ Invalid device token format: {}", recipient);
                return false;
            }

            // Simulate push sending delay
            Thread.sleep(500); // 500ms delay

            // Mock success with 90% rate for demo
            boolean success = Math.random() > 0.1;

            if (success) {
                log.info("✅ Push notification sent successfully to device: {}", recipient);
                return true;
            } else {
                log.warn("⚠️ Push notification sending failed to device: {} (mock failure)", recipient);
                return false;
            }

        } catch (Exception e) {
            log.error("💥 Failed to send push notification to {}: {}", recipient, e.getMessage());
            return false;
        }
    }

    /**
     * Validate device token format (simplified validation)
     */
    private boolean isValidDeviceToken(String token) {
        return token != null && !token.trim().isEmpty() && token.length() >= 50 && token.length() <= 200;
    }

    @Override
    public boolean isHealthy() {
        try {
            // Mock health check
            log.info("📱 Push service health check - OK");
            return true;

        } catch (Exception e) {
            log.error("💥 Push service health check failed: {}", e.getMessage());
            return false;
        }
    }
}