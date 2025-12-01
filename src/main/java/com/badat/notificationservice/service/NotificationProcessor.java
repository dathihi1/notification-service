package com.badat.notificationservice.service;

import com.badat.notificationservice.entity.NotificationMessage;
import com.badat.notificationservice.repository.NotificationRepository;
import com.badat.notificationservice.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Notification Processor Pipeline
 * Core processing engine for multi-channel notifications
 * Handles queuing, processing, retry logic, and delivery tracking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    private final NotificationRepository notificationRepository;
    private final Map<String, NotificationSender> notificationSenders;

    /**
     * Process a single notification
     * Main entry point for notification processing
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public CompletableFuture<Boolean> processNotification(String notificationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🔄 Processing notification: {}", notificationId);

                // Get notification from database
                NotificationMessage notification = notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

                // Update status to PROCESSING
                notification.setStatus(NotificationMessage.NotificationStatus.PROCESSING);
                notification.setUpdatedAt(LocalDateTime.now());
                notificationRepository.save(notification);

                // Get appropriate sender for channel
                NotificationSender sender = notificationSenders.get(notification.getChannelType().toUpperCase());
                if (sender == null) {
                    throw new RuntimeException("No sender found for channel: " + notification.getChannelType());
                }

                // Check sender health before processing
                if (!sender.isHealthy()) {
                    throw new RuntimeException(
                            "Notification sender not healthy for channel: " + notification.getChannelType());
                }

                // Prepare metadata
                Map<String, Object> metadata = notification.getMetadataAsMap();

                // Send notification
                boolean success = sender.send(
                        notification.getRecipient(),
                        notification.getTitle(),
                        notification.getContent(),
                        metadata);

                // Update notification based on result
                if (success) {
                    markAsSent(notification);
                    log.info("✅ Notification processed successfully: {}", notificationId);
                } else {
                    markAsFailed(notification, "Sender returned false");
                    log.warn("⚠️ Notification processing failed: {}", notificationId);
                }

                return success;

            } catch (Exception e) {
                log.error("💥 Error processing notification {}: {}", notificationId, e.getMessage(), e);
                markAsFailed(notificationId, e.getMessage());
                return false;
            }
        });
    }

    /**
     * Process batch of notifications
     * For bulk processing operations
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public CompletableFuture<Integer> processBatch(List<String> notificationIds) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("🔄 Processing batch of {} notifications", notificationIds.size());

            int successCount = 0;
            for (String notificationId : notificationIds) {
                try {
                    Boolean result = processNotification(notificationId).get();
                    if (result) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("💥 Error in batch processing for notification {}: {}", notificationId, e.getMessage());
                }
            }

            log.info("✅ Batch processing completed: {}/{} successful", successCount, notificationIds.size());
            return successCount;
        });
    }

    /**
     * Retry failed notifications
     * For retry operations from failed queue
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public CompletableFuture<Boolean> retryNotification(String notificationId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("🔄 Retrying notification: {}", notificationId);

                NotificationMessage notification = notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

                // Check retry limit
                if (notification.getRetryCount() >= notification.getMaxRetries()) {
                    log.warn("⚠️ Max retries exceeded for notification: {}", notificationId);
                    markAsExpired(notification);
                    return false;
                }

                // Increment retry count
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setStatus(NotificationMessage.NotificationStatus.PENDING);
                notification.setError(null);
                notification.setUpdatedAt(LocalDateTime.now());
                notificationRepository.save(notification);

                // Process again
                return processNotification(notificationId).get();

            } catch (Exception e) {
                log.error("💥 Error retrying notification {}: {}", notificationId, e.getMessage());
                markAsFailed(notificationId, "Retry failed: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Schedule notification for future delivery
     * For delayed/scheduled notifications
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public CompletableFuture<String> scheduleNotification(
            String recipient,
            String channelType,
            String title,
            String content,
            LocalDateTime scheduledFor,
            Map<String, Object> metadata) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("⏰ Scheduling notification for: {} at {}", recipient, scheduledFor);

                NotificationMessage notification = NotificationMessage.builder()
                        .recipient(recipient)
                        .channelType(channelType)
                        .title(title)
                        .content(content)
                        .priority(NotificationMessage.NotificationPriority.NORMAL)
                        .status(NotificationMessage.NotificationStatus.PENDING)
                        .metadata(null) // Will be set below
                        .build();

                // Set metadata if provided
                if (metadata != null && !metadata.isEmpty()) {
                    notification.setMetadataFromMap(metadata);
                }

                // Add scheduled timestamp to metadata
                Map<String, Object> finalMetadata = notification.getMetadataAsMap();
                finalMetadata.put("scheduledFor", scheduledFor.toString());
                notification.setMetadataFromMap(finalMetadata);

                // Save to database
                NotificationMessage saved = notificationRepository.save(notification);

                log.info("✅ Notification scheduled: {} for {}", saved.getId(), scheduledFor);
                return saved.getId();

            } catch (Exception e) {
                log.error("💥 Error scheduling notification: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to schedule notification", e);
            }
        });
    }

    /**
     * Cancel scheduled notification
     */
    @Transactional
    public boolean cancelNotification(String notificationId) {
        try {
            log.info("❌ Cancelling notification: {}", notificationId);

            NotificationMessage notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));

            // Only cancel if not already processed
            if (notification.getStatus() == NotificationMessage.NotificationStatus.PENDING) {
                notification.setStatus(NotificationMessage.NotificationStatus.CANCELLED);
                notification.setUpdatedAt(LocalDateTime.now());
                notificationRepository.save(notification);

                log.info("✅ Notification cancelled: {}", notificationId);
                return true;
            } else {
                log.warn("⚠️ Cannot cancel notification - already processed: {}", notificationId);
                return false;
            }

        } catch (Exception e) {
            log.error("💥 Error cancelling notification {}: {}", notificationId, e.getMessage());
            return false;
        }
    }

    /**
     * Mark notification as sent successfully
     */
    private void markAsSent(NotificationMessage notification) {
        notification.setStatus(NotificationMessage.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
        notification.setError(null);
        notificationRepository.save(notification);
    }

    /**
     * Mark notification as failed with error message
     */
    private void markAsFailed(NotificationMessage notification, String errorMessage) {
        notification.setStatus(NotificationMessage.NotificationStatus.FAILED);
        notification.setError(errorMessage);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /**
     * Mark notification as failed by ID (for exception handling)
     */
    private void markAsFailed(String notificationId, String errorMessage) {
        try {
            NotificationMessage notification = notificationRepository.findById(notificationId).orElse(null);
            if (notification != null) {
                markAsFailed(notification, errorMessage);
            }
        } catch (Exception e) {
            log.error("💥 Error marking notification as failed {}: {}", notificationId, e.getMessage());
        }
    }

    /**
     * Mark notification as expired (max retries exceeded)
     */
    private void markAsExpired(NotificationMessage notification) {
        notification.setStatus(NotificationMessage.NotificationStatus.EXPIRED);
        notification.setError("Max retries exceeded");
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}