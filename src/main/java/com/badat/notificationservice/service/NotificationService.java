package com.badat.notificationservice.service;

import com.badat.notificationservice.dto.request.NotificationRequest;
import com.badat.notificationservice.dto.response.NotificationResponse;
import com.badat.notificationservice.entity.NotificationMessage;
import com.badat.notificationservice.repository.NotificationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core Notification Service - Multi-Channel Support
 * Handles requests from external services and processes via @Scheduled worker
 * Supports EMAIL, SMS, PUSH, WEBHOOK, IN_APP channels
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class NotificationService {

    NotificationRepository notificationRepository;
    NotificationQueueManager notificationQueueManager;

    /**
     * Notification Channel Types
     */
    public enum ChannelType {
        EMAIL,
        SMS,
        PUSH_NOTIFICATION,
        WEBHOOK,
        IN_APP
    }

    /**
     * Recipient Types
     */
    public enum RecipientType {
        EMAIL,
        PHONE,
        DEVICE_TOKEN,
        WEBHOOK_URL,
        USER_ID
    }

    /**
     * Constructor for initializing notification senders
     */
    public NotificationService(NotificationRepository notificationRepository,
            NotificationQueueManager notificationQueueManager) {
        this.notificationRepository = notificationRepository;
        this.notificationQueueManager = notificationQueueManager;
    }

    /**
     * Receive notification from external service
     * REST API endpoint for other services to send notifications
     */
    @Transactional
    public NotificationResponse receiveNotification(NotificationRequest request) {
        try {
            log.info("📥 Received notification request: {} -> {} via {}",
                    request.getRecipientType(), request.getRecipient(), request.getChannelType());

            // Validate recipient type matches channel type
            validateRecipientChannelMatch(request);

            // Create notification entity
            NotificationMessage notification = NotificationMessage.builder()
                    .recipientType(request.getRecipientType())
                    .recipient(request.getRecipient())
                    .channelType(request.getChannelType())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .priority(NotificationMessage.NotificationPriority.valueOf(request.getPriority()))
                    .templateId(request.getTemplateId())
                    .metadata(request.getMetadata().toString())
                    .status(NotificationMessage.NotificationStatus.PENDING)
                    .retryCount(0)
                    .build();

            // Save to database
            notificationRepository.save(notification);

            // Push to Redis Queue
            notificationQueueManager.addToQueue(notification);

            // Return response
            NotificationResponse response = NotificationResponse.builder()
                    .id(notification.getId())
                    .status(notification.getStatus().name())
                    .recipientType(notification.getRecipientType())
                    .recipient(notification.getRecipient())
                    .channelType(notification.getChannelType())
                    .createdAt(notification.getCreatedAt())
                    .priority(notification.getPriority().toString())
                    .templateId(notification.getTemplateId())
                    .build();

            log.info("✅ Successfully received notification request: {}", notification.getId());

            return response;

        } catch (Exception e) {
            log.error("💥 Error receiving notification request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process notification request", e);
        }
    }

    /**
     * Get notifications by status with pagination
     */
    public List<NotificationMessage> getNotificationsByStatus(String status, int limit) {
        return notificationRepository.findByStatusOrderByCreatedAtDesc(status, limit);
    }

    /**
     * Get notifications by recipient
     */
    public List<NotificationMessage> getNotificationsByRecipient(String recipient, int limit) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public List<NotificationMessage> getNotificationsByChannelType(String channelType, int limit) {
        return notificationRepository.findByChannelTypeOrderByCreatedAtDesc(channelType, limit);
    }

    /**
     * Validate recipient type matches channel type
     */
    private void validateRecipientChannelMatch(NotificationRequest request) {
        switch (request.getChannelType()) {
            case "EMAIL":
                if (!"EMAIL".equals(request.getRecipientType())) {
                    throw new IllegalArgumentException("EMAIL channel requires EMAIL recipient type");
                }
                break;
            case "SMS":
                if (!"PHONE".equals(request.getRecipientType())) {
                    throw new IllegalArgumentException("SMS channel requires PHONE recipient type");
                }
                break;
            case "PUSH_NOTIFICATION":
                if (!"DEVICE_TOKEN".equals(request.getRecipientType())) {
                    throw new IllegalArgumentException("PUSH channel requires DEVICE_TOKEN recipient type");
                }
                break;
            case "WEBHOOK":
                if (!"WEBHOOK_URL".equals(request.getRecipientType())) {
                    throw new IllegalArgumentException("WEBHOOK channel requires WEBHOOK_URL recipient type");
                }
                break;
            case "IN_APP":
                if (!"USER_ID".equals(request.getRecipientType())) {
                    throw new IllegalArgumentException("IN_APP channel requires USER_ID recipient type");
                }
                break;
        }
    }

    /**
     * Cleanup old notifications - runs every hour
     */
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void cleanupOldNotifications() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // 7 days ago
            List<NotificationMessage> oldNotifications = notificationRepository
                    .findOldNotificationsForCleanup(
                            cutoff,
                            List.of(NotificationMessage.NotificationStatus.SENT,
                                    NotificationMessage.NotificationStatus.FAILED),
                            1000);

            if (!oldNotifications.isEmpty()) {
                log.info("🗑️  Cleaning up {} old notifications", oldNotifications.size());

                List<String> idsToDelete = oldNotifications.stream()
                        .map(NotificationMessage::getId)
                        .toList();

                notificationRepository.deleteByIdIn(idsToDelete);

                log.info("✅ Cleaned up {} old notifications", idsToDelete.size());
            }

        } catch (Exception e) {
            log.error("💥 Error during notification cleanup: {}", e.getMessage(), e);
        }
    }
}