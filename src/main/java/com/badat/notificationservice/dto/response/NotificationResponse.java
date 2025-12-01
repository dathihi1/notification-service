package com.badat.notificationservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Standardized Notification Response DTO
 * Consistent response format for all notification channels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class NotificationResponse {

    /**
     * Notification unique identifier
     */
    private String id;

    /**
     * Current status of the notification
     */
    private String status;

    /**
     * Type of recipient (EMAIL, PHONE, DEVICE_TOKEN, etc.)
     */
    private String recipientType;

    /**
     * Recipient address (email, phone, device ID, etc.)
     */
    private String recipient;

    /**
     * Channel used for delivery
     */
    private String channelType;

    /**
     * Creation timestamp of the notification
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * When notification was successfully sent
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    /**
     * Error message if notification failed
     */
    private String error;

    /**
     * Number of retry attempts
     */
    private Integer retryCount;

    /**
     * Delivery confirmation (for channels that support it)
     */
    private Boolean delivered;

    /**
     * Channel-specific delivery details
     */
    private String deliveryDetails;

    /**
     * Notification priority level
     */
    private String priority;

    /**
     * Template identifier if template was used
     */
    private String templateId;
}