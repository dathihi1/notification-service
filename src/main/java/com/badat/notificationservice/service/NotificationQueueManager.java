package com.badat.notificationservice.service;

import com.badat.notificationservice.entity.NotificationMessage;
import com.badat.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Notification Queue Manager
 * Manages Redis queues for notifications
 * Handles priority queues, delayed delivery, and queue monitoring
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationQueueManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationRepository notificationRepository;
    private final NotificationProcessor notificationProcessor;
    private final ObjectMapper objectMapper;

    // Redis Keys
    private static final String QUEUE_PREFIX = "notifications:queue:";
    private static final String PROCESSING_PREFIX = "notifications:processing:"; // ZSET: member=notificationId,
                                                                                 // score=expirationTime
    private static final String FAILED_PREFIX = "notifications:failed:";
    private static final String DELAYED_PREFIX = "notifications:delayed:"; // ZSET: member=notificationId,
                                                                           // score=deliveryTime
    private static final String STATS_PREFIX = "notifications:stats:";

    /**
     * Add notification to appropriate priority queue
     */
    public boolean addToQueue(NotificationMessage notification) {
        try {
            String queueKey = getQueueKey(notification.getPriority());
            String notificationJson = objectMapper.writeValueAsString(notification);

            // Add to priority queue
            redisTemplate.opsForList().rightPush(queueKey, notificationJson);

            // Update stats
            incrementStat("pending", notification.getChannelType());

            log.info("✅ Added to {} queue: {}", notification.getPriority(), notification.getId());
            return true;

        } catch (Exception e) {
            log.error("💥 Error adding notification to queue {}: {}", notification.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Add notification to delayed queue for future delivery
     */
    public boolean addToDelayedQueue(NotificationMessage notification, LocalDateTime deliverAt) {
        try {
            String delayedKey = DELAYED_PREFIX + "zset";
            String notificationJson = objectMapper.writeValueAsString(notification);

            // Store notification data in a separate key
            String dataKey = DELAYED_PREFIX + "data:" + notification.getId();
            redisTemplate.opsForValue().set(dataKey, notificationJson);

            // Add to ZSET with delivery time as score
            long deliveryTimestamp = deliverAt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
            redisTemplate.opsForZSet().add(delayedKey, notification.getId(), deliveryTimestamp);

            log.info("⏰ Added to delayed queue: {} at {}", notification.getId(), deliverAt);
            return true;

        } catch (Exception e) {
            log.error("💥 Error adding notification to delayed queue {}: {}", notification.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Get next notification from queue (priority order: URGENT > HIGH > NORMAL >
     * LOW)
     */
    @Async
    public NotificationMessage getNextNotification() {
        try {
            // Check queues in priority order
            List<String> priorities = List.of("URGENT", "HIGH", "NORMAL", "LOW");

            for (String priority : priorities) {
                String queueKey = QUEUE_PREFIX + priority;
                String notificationJson = (String) redisTemplate.opsForList().leftPop(queueKey, 1, TimeUnit.SECONDS);

                if (notificationJson != null) {
                    NotificationMessage notification = objectMapper.readValue(notificationJson,
                            NotificationMessage.class);

                    // Move to processing queue
                    moveToProcessing(notification);

                    // Update stats
                    incrementStat("processing", notification.getChannelType());
                    decrementStat("pending", notification.getChannelType());

                    log.info("🔄 Retrieved from queue: {} (priority: {})", notification.getId(), priority);
                    return notification;
                }
            }

            // No notifications available
            return null;

        } catch (Exception e) {
            log.error("💥 Error getting next notification: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Move notification to processing queue
     */
    private void moveToProcessing(NotificationMessage notification) {
        try {
            String processingZSetKey = PROCESSING_PREFIX + "zset";
            String processingDataKey = PROCESSING_PREFIX + "data:" + notification.getId();
            String notificationJson = objectMapper.writeValueAsString(notification);

            // Store data
            redisTemplate.opsForValue().set(processingDataKey, notificationJson, 5, TimeUnit.MINUTES);

            // Add to ZSET with expiration time (now + 5 mins)
            long expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
            redisTemplate.opsForZSet().add(processingZSetKey, notification.getId(), expirationTime);

        } catch (Exception e) {
            log.error("💥 Error moving notification to processing {}: {}", notification.getId(), e.getMessage());
        }
    }

    /**
     * Mark notification as processed (remove from processing queue)
     */
    public void markAsProcessed(String notificationId) {
        try {
            String processingZSetKey = PROCESSING_PREFIX + "zset";
            String processingDataKey = PROCESSING_PREFIX + "data:" + notificationId;

            redisTemplate.opsForZSet().remove(processingZSetKey, notificationId);
            redisTemplate.delete(processingDataKey);

            // Update stats
            incrementStat("completed", "all");

            log.info("✅ Marked as processed: {}", notificationId);

        } catch (Exception e) {
            log.error("💥 Error marking notification as processed {}: {}", notificationId, e.getMessage());
        }
    }

    /**
     * Move failed notification to failed queue
     */
    public void markAsFailed(NotificationMessage notification, String errorMessage) {
        try {
            String failedKey = FAILED_PREFIX + notification.getId();
            String notificationJson = objectMapper.writeValueAsString(notification);

            // Add to failed queue with error info
            redisTemplate.opsForList().rightPush(failedKey, notificationJson);

            // Remove from processing queue
            String processingZSetKey = PROCESSING_PREFIX + "zset";
            String processingDataKey = PROCESSING_PREFIX + "data:" + notification.getId();

            redisTemplate.opsForZSet().remove(processingZSetKey, notification.getId());
            redisTemplate.delete(processingDataKey);

            // Update stats
            incrementStat("failed", notification.getChannelType());
            decrementStat("processing", notification.getChannelType());

            log.warn("⚠️ Marked as failed: {} - {}", notification.getId(), errorMessage);

        } catch (Exception e) {
            log.error("💥 Error marking notification as failed {}: {}", notification.getId(), e.getMessage());
        }
    }

    /**
     * Get queue sizes
     */
    public QueueStats getQueueStats() {
        try {
            QueueStats stats = new QueueStats();

            // Count pending by priority
            stats.setUrgentCount(redisTemplate.opsForList().size(QUEUE_PREFIX + "URGENT"));
            stats.setHighCount(redisTemplate.opsForList().size(QUEUE_PREFIX + "HIGH"));
            stats.setNormalCount(redisTemplate.opsForList().size(QUEUE_PREFIX + "NORMAL"));
            stats.setLowCount(redisTemplate.opsForList().size(QUEUE_PREFIX + "LOW"));

            // Count processing (use ZSET size)
            Long processingCount = redisTemplate.opsForZSet().size(PROCESSING_PREFIX + "zset");
            stats.setProcessingCount(processingCount != null ? processingCount : 0L);

            // Count failed (use LIST size)
            // Note: This is an approximation or we need a separate counter for total failed
            // For now, we rely on the stats counter we maintain
            stats.setFailedCount(getStatCount("failed", "all"));

            // Get counts from stats
            stats.setPendingEmail(getStatCount("pending", "EMAIL"));
            stats.setPendingSms(getStatCount("pending", "SMS"));
            stats.setProcessingEmail(getStatCount("processing", "EMAIL"));
            stats.setProcessingSms(getStatCount("processing", "SMS"));
            stats.setCompletedEmail(getStatCount("completed", "EMAIL"));
            stats.setCompletedSms(getStatCount("completed", "SMS"));
            stats.setFailedEmail(getStatCount("failed", "EMAIL"));
            stats.setFailedSms(getStatCount("failed", "SMS"));

            return stats;

        } catch (Exception e) {
            log.error("💥 Error getting queue stats: {}", e.getMessage());
            return new QueueStats();
        }
    }

    /**
     * Scheduled task to process notifications continuously
     */
    @Scheduled(fixedDelay = 1000) // Process every second
    public void processNotificationsContinuously() {
        try {
            NotificationMessage notification = getNextNotification();
            if (notification != null) {
                // Process asynchronously
                notificationProcessor.processNotification(notification.getId())
                        .thenAccept(success -> {
                            if (success) {
                                markAsProcessed(notification.getId());
                            } else {
                                markAsFailed(notification, "Processing failed");
                            }
                        });
            }

        } catch (Exception e) {
            log.error("💥 Error in continuous notification processing: {}", e.getMessage());
        }
    }

    /**
     * Scheduled task to move delayed notifications to regular queue
     */
    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    public void processDelayedNotifications() {
        try {
            String delayedKey = DELAYED_PREFIX + "zset";
            long now = System.currentTimeMillis() / 1000;

            // Get items due for delivery (score <= now)
            Set<Object> dueNotifications = redisTemplate.opsForZSet().rangeByScore(delayedKey, 0, now);

            if (dueNotifications != null && !dueNotifications.isEmpty()) {
                for (Object obj : dueNotifications) {
                    String notificationId = (String) obj;
                    String dataKey = DELAYED_PREFIX + "data:" + notificationId;

                    try {
                        String notificationJson = (String) redisTemplate.opsForValue().get(dataKey);
                        if (notificationJson != null) {
                            NotificationMessage notification = objectMapper.readValue(notificationJson,
                                    NotificationMessage.class);

                            // Move to regular queue
                            addToQueue(notification);

                            // Cleanup
                            redisTemplate.delete(dataKey);
                            redisTemplate.opsForZSet().remove(delayedKey, notificationId);

                            log.info("⏰ Moved delayed notification to queue: {}", notificationId);
                        } else {
                            // Data missing, just remove from ZSET
                            redisTemplate.opsForZSet().remove(delayedKey, notificationId);
                        }
                    } catch (Exception e) {
                        log.error("💥 Error processing delayed notification {}: {}", notificationId, e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("💥 Error in delayed notification processing: {}", e.getMessage());
        }
    }

    /**
     * Scheduled task to clean up expired processing notifications
     */
    @Scheduled(fixedDelay = 60000) // Clean up every minute
    public void cleanupExpiredProcessing() {
        try {
            String processingZSetKey = PROCESSING_PREFIX + "zset";
            long now = System.currentTimeMillis();

            // Get expired items (score < now)
            Set<Object> expiredProcessing = redisTemplate.opsForZSet().rangeByScore(processingZSetKey, 0, now);

            if (expiredProcessing != null) {
                for (Object obj : expiredProcessing) {
                    String notificationId = (String) obj;
                    String dataKey = PROCESSING_PREFIX + "data:" + notificationId;

                    try {
                        // Check if notification still exists in database
                        NotificationMessage notification = notificationRepository.findById(notificationId).orElse(null);

                        if (notification == null ||
                                notification.getStatus() == NotificationMessage.NotificationStatus.SENT ||
                                notification.getStatus() == NotificationMessage.NotificationStatus.FAILED) {

                            // Remove from processing queue
                            redisTemplate.delete(dataKey);
                            redisTemplate.opsForZSet().remove(processingZSetKey, notificationId);
                            log.debug("🧹 Cleaned up processing key: {}", notificationId);
                        } else {
                            // Still pending/processing in DB but expired in Redis - maybe re-queue?
                            // For now just extend timeout or leave it for manual intervention
                            log.warn("⚠️ Found expired processing item that is not done in DB: {}", notificationId);
                        }

                    } catch (Exception e) {
                        log.error("💥 Error cleaning up processing key {}: {}", notificationId, e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("💥 Error in processing cleanup: {}", e.getMessage());
        }
    }

    // Helper methods
    private String getQueueKey(NotificationMessage.NotificationPriority priority) {
        return QUEUE_PREFIX + priority.toString();
    }

    private void incrementStat(String status, String channel) {
        String key = STATS_PREFIX + status + ":" + (channel != null ? channel : "all");
        redisTemplate.opsForValue().increment(key);
    }

    private void decrementStat(String status, String channel) {
        String key = STATS_PREFIX + status + ":" + (channel != null ? channel : "all");
        redisTemplate.opsForValue().decrement(key);
    }

    private Long getStatCount(String status, String channel) {
        String key = STATS_PREFIX + status + ":" + channel;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : 0L;
    }

    /**
     * Queue Statistics DTO
     */
    public static class QueueStats {
        private Long urgentCount = 0L;
        private Long highCount = 0L;
        private Long normalCount = 0L;
        private Long lowCount = 0L;
        private Long processingCount = 0L;
        private Long failedCount = 0L;
        private Long pendingEmail = 0L;
        private Long pendingSms = 0L;
        private Long processingEmail = 0L;
        private Long processingSms = 0L;
        private Long completedEmail = 0L;
        private Long completedSms = 0L;
        private Long failedEmail = 0L;
        private Long failedSms = 0L;

        // Getters and setters
        public Long getUrgentCount() {
            return urgentCount;
        }

        public void setUrgentCount(Long urgentCount) {
            this.urgentCount = urgentCount;
        }

        public Long getHighCount() {
            return highCount;
        }

        public void setHighCount(Long highCount) {
            this.highCount = highCount;
        }

        public Long getNormalCount() {
            return normalCount;
        }

        public void setNormalCount(Long normalCount) {
            this.normalCount = normalCount;
        }

        public Long getLowCount() {
            return lowCount;
        }

        public void setLowCount(Long lowCount) {
            this.lowCount = lowCount;
        }

        public Long getProcessingCount() {
            return processingCount;
        }

        public void setProcessingCount(Long processingCount) {
            this.processingCount = processingCount;
        }

        public Long getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(Long failedCount) {
            this.failedCount = failedCount;
        }

        public Long getPendingEmail() {
            return pendingEmail;
        }

        public void setPendingEmail(Long pendingEmail) {
            this.pendingEmail = pendingEmail;
        }

        public Long getPendingSms() {
            return pendingSms;
        }

        public void setPendingSms(Long pendingSms) {
            this.pendingSms = pendingSms;
        }

        public Long getProcessingEmail() {
            return processingEmail;
        }

        public void setProcessingEmail(Long processingEmail) {
            this.processingEmail = processingEmail;
        }

        public Long getProcessingSms() {
            return processingSms;
        }

        public void setProcessingSms(Long processingSms) {
            this.processingSms = processingSms;
        }

        public Long getCompletedEmail() {
            return completedEmail;
        }

        public void setCompletedEmail(Long completedEmail) {
            this.completedEmail = completedEmail;
        }

        public Long getCompletedSms() {
            return completedSms;
        }

        public void setCompletedSms(Long completedSms) {
            this.completedSms = completedSms;
        }

        public Long getFailedEmail() {
            return failedEmail;
        }

        public void setFailedEmail(Long failedEmail) {
            this.failedEmail = failedEmail;
        }

        public Long getFailedSms() {
            return failedSms;
        }

        public void setFailedSms(Long failedSms) {
            this.failedSms = failedSms;
        }
    }
}