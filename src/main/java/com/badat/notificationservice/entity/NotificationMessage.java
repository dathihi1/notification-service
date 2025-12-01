package com.badat.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized Notification Message Entity
 * Multi-channel support: EMAIL, SMS, PUSH, WEBHOOK, IN_APP
 * Follows enterprise patterns with proper validation and tracking
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "notifications")
public class NotificationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 20)
    private String recipientType;

    /**
     * Recipient address - validated per type
     */
    @Column(nullable = false, length = 500)
    private String recipient;

    @Column(nullable = false, length = 50)
    private String channelType;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Notification Content - supports HTML/text for emails, text for SMS/push
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Notification Status Lifecycle
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    /**
     * Processing Priority for queue management
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    /**
     * Creation timestamp for audit and ordering
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Update timestamp for optimistic locking and audit
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, updatable = false)
    private LocalDateTime updatedAt;

    /**
     * Optional template identifier for templated notifications
     */
    @Column(length = 100)
    private String templateId;

    /**
     * Flexible metadata for channel-specific data
     * JSON format for extensibility
     */
    @Column(length = 2000)
    private String metadata;

    /**
     * When the notification was successfully sent
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Error information for failed notifications
     */
    @Column(length = 1000)
    private String error;

    /**
     * Retry counter for failed notifications
     */
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Maximum retry attempts allowed
     */
    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Notification Status Enum
     */
    public enum NotificationStatus {
        PENDING,
        PROCESSING,
        SENT,
        FAILED,
        CANCELLED,
        EXPIRED
    }

    /**
     * Notification Priority Enum
     */
    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    /**
     * Channel Type Constants
     */
    public static class ChannelType {
        public static final String EMAIL = "EMAIL";
        public static final String SMS = "SMS";
        public static final String PUSH_NOTIFICATION = "PUSH_NOTIFICATION";
        public static final String WEBHOOK = "WEBHOOK";
        public static final String IN_APP = "IN_APP";
    }

    /**
     * Recipient Type Constants
     */
    public static class RecipientType {
        public static final String EMAIL = "EMAIL";
        public static final String PHONE = "PHONE";
        public static final String DEVICE_TOKEN = "DEVICE_TOKEN";
        public static final String WEBHOOK_URL = "WEBHOOK_URL";
        public static final String USER_ID = "USER_ID";
    }

    /**
     * Convert metadata JSON String to Map for type safety
     */
    @Transient
    public Map<String, Object> getMetadataAsMap() {
        if (metadata == null || metadata.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return new ObjectMapper().readValue(metadata, Map.class);
        } catch (Exception e) {
            return Map.of("error", "Invalid metadata format: " + e.getMessage());
        }
    }

    /**
     * Set metadata from Map for convenience
     */
    public void setMetadataFromMap(Map<String, Object> metadataMap) {
        if (metadataMap == null || metadataMap.isEmpty()) {
            this.metadata = null;
        } else {
            try {
                this.metadata = new ObjectMapper().writeValueAsString(metadataMap);
            } catch (Exception e) {
                this.metadata = "{\"error\": \"Failed to serialize metadata\"}";
            }
        }
    }
}