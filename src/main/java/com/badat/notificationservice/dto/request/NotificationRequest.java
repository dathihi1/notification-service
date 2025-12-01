package com.badat.notificationservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Standardized Notification Request DTO
 * Multi-channel support with comprehensive validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class NotificationRequest {

    @NotBlank(message = "Recipient type is required")
    @Size(max = 20, message = "Recipient type must be EMAIL, PHONE, DEVICE_TOKEN, WEBHOOK_URL, or USER_ID")
    private String recipientType;

    @NotBlank(message = "Recipient is required")
    @Size(max = 500, message = "Recipient must be less than 500 characters")
    private String recipient;

    @NotBlank(message = "Channel type is required")
    @Size(max = 50, message = "Channel type must be EMAIL, SMS, PUSH_NOTIFICATION, WEBHOOK, or IN_APP")
    private String channelType;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be less than 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must be less than 5000 characters")
    private String content;

    @Builder.Default
    private String priority = "NORMAL";

    @Builder.Default
    private Integer delaySeconds = 0;

    @Size(max = 100, message = "Template ID must be less than 100 characters")
    private String templateId;

    /**
     * Flexible metadata for channel-specific data
     * Examples:
     * - Email: {"cc": ["admin@example.com"], "bcc": ["support@example.com"]}
     * - SMS: {"countryCode": "+84", "carrier": "viettel"}
     * - Push: {"deviceId": "device_123", "platform": "ios/android"}
     * - Webhook: {"headers": {"Authorization": "Bearer token"}, "timeout": 30000}
     * - InApp: {"userId": "user_123", "sessionId": "session_456"}
     */
    private Map<String, Object> metadata;
}