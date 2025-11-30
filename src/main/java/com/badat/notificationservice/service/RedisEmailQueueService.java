package com.badat.notificationservice.service;

import com.badat.notificationservice.dto.EmailMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RedisEmailQueueService {

    RedisTemplate<String, Object> redisTemplate;

    // Redis keys
    private static final String EMAIL_QUEUE = "email:queue";
    private static final String EMAIL_PROCESSING = "email:processing";
    private static final String EMAIL_FAILED = "email:failed";
    private static final String EMAIL_STATS = "email:stats";

    /**
     * Thêm email vào queue
     */
    public void addToQueue(EmailMessage message) {
        try {
            redisTemplate.opsForList().rightPush(EMAIL_QUEUE, message);
            updateStats("queued", 1);
            log.info("Added email to queue: {} -> {}", message.getId(), message.getRecipient());
        } catch (Exception e) {
            log.error("Error adding email to queue: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add email to queue", e);
        }
    }

    /**
     * Lấy email tiếp theo từ queue (atomic operation)
     */
    public synchronized Optional<EmailMessage> getNextTask() {
        try {
            // Pop from queue
            EmailMessage message = (EmailMessage) redisTemplate.opsForList().rightPop(EMAIL_QUEUE);

            if (message != null) {
                // Add to processing queue
                redisTemplate.opsForList().rightPush(EMAIL_PROCESSING, message);
                redisTemplate.opsForSet().add(EMAIL_PROCESSING + ":ids", message.getId());
                updateStats("processing", 1);
                log.info("Retrieved email from queue: {}", message.getId());
                return Optional.of(message);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting next task: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Đánh dấu email đã gửi thành công
     */
    public void markAsCompleted(String messageId) {
        try {
            // Remove from processing set
            redisTemplate.opsForSet().remove(EMAIL_PROCESSING + ":ids", messageId);

            // Remove from processing list (if exists)
            redisTemplate.opsForList().remove(EMAIL_PROCESSING, 1, messageId);

            updateStats("completed", 1);
            log.info("Marked email as completed: {}", messageId);
        } catch (Exception e) {
            log.error("Error marking email as completed {}: {}", messageId, e.getMessage(), e);
        }
    }

    /**
     * Đánh dấu email gửi thất bại và thêm vào failed queue
     */
    public void markAsFailed(String messageId, EmailMessage failedMessage) {
        try {
            // Increment retry count
            failedMessage.setRetryCount(failedMessage.getRetryCount() + 1);

            if (failedMessage.getRetryCount() < 3) {
                // Retry: add back to queue
                redisTemplate.opsForList().rightPush(EMAIL_QUEUE, failedMessage);
                log.info("Retrying email {} (attempt {})", messageId, failedMessage.getRetryCount());
            } else {
                // Max retry reached: add to failed queue
                redisTemplate.opsForList().rightPush(EMAIL_FAILED, failedMessage);
                log.error("Email {} failed permanently after {} attempts", messageId, failedMessage.getRetryCount());
            }

            // Remove from processing
            redisTemplate.opsForSet().remove(EMAIL_PROCESSING + ":ids", messageId);
            redisTemplate.opsForList().remove(EMAIL_PROCESSING, 1, messageId);

            updateStats("failed", 1);
        } catch (Exception e) {
            log.error("Error marking email as failed {}: {}", messageId, e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách emails đang bị lỗi
     */
    public List<EmailMessage> getFailedEmails() {
        try {
            List<Object> failed = redisTemplate.opsForList().range(EMAIL_FAILED, 0, -1);
            return failed.stream()
                    .map(obj -> (EmailMessage) obj)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting failed emails: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy danh sách email IDs đang được xử lý
     */
    public Set<Object> getProcessingIds() {
        try {
            return redisTemplate.opsForSet().members(EMAIL_PROCESSING + ":ids");
        } catch (Exception e) {
            log.error("Error getting processing IDs: {}", e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Kiểm tra email có đang được xử lý không
     */
    public boolean isProcessing(String messageId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(EMAIL_PROCESSING + ":ids", messageId));
        } catch (Exception e) {
            log.error("Error checking processing status for {}: {}", messageId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Clear processing queue (cho recovery)
     */
    public void clearProcessing() {
        try {
            Set<Object> processingIds = redisTemplate.opsForSet().members(EMAIL_PROCESSING + ":ids");
            if (processingIds != null) {
                processingIds.forEach(id -> {
                    redisTemplate.opsForList().remove(EMAIL_PROCESSING, 1, id);
                });
            }
            redisTemplate.delete(EMAIL_PROCESSING + ":ids");
            log.info("Cleared processing queue");
        } catch (Exception e) {
            log.error("Error clearing processing queue: {}", e.getMessage(), e);
        }
    }

    // Monitoring methods
    public long getQueueSize() {
        return redisTemplate.opsForList().size(EMAIL_QUEUE) != null ?
               redisTemplate.opsForList().size(EMAIL_QUEUE) : 0;
    }

    public long getProcessingCount() {
        return redisTemplate.opsForSet().size(EMAIL_PROCESSING + ":ids") != null ?
               redisTemplate.opsForSet().size(EMAIL_PROCESSING + ":ids") : 0;
    }

    public long getFailedCount() {
        return redisTemplate.opsForList().size(EMAIL_FAILED) != null ?
               redisTemplate.opsForList().size(EMAIL_FAILED) : 0;
    }

    private void updateStats(String operation, long count) {
        try {
            redisTemplate.opsForHash().increment(EMAIL_STATS, operation, count);
        } catch (Exception e) {
            log.error("Error updating stats: {}", e.getMessage());
        }
    }
}