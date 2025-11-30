package com.badat.notificationservice.configuration;

import com.badat.notificationservice.dto.EmailMessage;
import com.badat.notificationservice.service.RedisEmailQueueService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableScheduling
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RedisEmailWorker {

    JavaMailSender mailSender;
    RedisEmailQueueService redisQueueService;

    // Counter for processed emails
    private final AtomicInteger processedCount = new AtomicInteger(0);

    /**
     * Worker processes emails every 1 second (faster than MySQL's 5 seconds)
     */
    @Scheduled(fixedDelay = 1000) // 1 second
    public void processEmailQueue() {
        try {
            // Get next email from queue
            var emailOptional = redisQueueService.getNextTask();

            if (emailOptional.isEmpty()) {
                // No emails to process
                log.debug("No emails in queue to process");
                return;
            }

            EmailMessage emailMessage = emailOptional.get();
            log.info("Processing email {} -> {} (attempt {})",
                    emailMessage.getId(), emailMessage.getRecipient(), emailMessage.getRetryCount() + 1);

            try {
                // Send email
                sendEmail(emailMessage.getRecipient(), emailMessage.getSubject(), emailMessage.getBody());

                // Mark as completed
                redisQueueService.markAsCompleted(emailMessage.getId());

                // Update counter
                int count = processedCount.incrementAndGet();
                log.info("Successfully sent email #{}: {} -> {}", count, emailMessage.getId(), emailMessage.getRecipient());

            } catch (Exception e) {
                log.error("Failed to send email {}: {}", emailMessage.getId(), e.getMessage());

                // Handle failure - will be retried or marked as failed permanently
                redisQueueService.markAsFailed(emailMessage.getId(), emailMessage);
            }

        } catch (Exception e) {
            log.error("Error in email worker: {}", e.getMessage(), e);
        }
    }

    /**
     * Send email using Spring Mail
     */
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage());
            throw e; // Re-throw to let worker handle retry logic
        }
    }

    /**
     * Cleanup task: runs every 5 minutes to clear stuck processing emails
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void cleanupStuckEmails() {
        try {
            var processingIds = redisQueueService.getProcessingIds();

            if (!processingIds.isEmpty()) {
                log.warn("Found {} emails stuck in processing set, cleaning up...", processingIds.size());
                // In production, you might want to handle these differently
                // For now, just log and clear
                redisQueueService.clearProcessing();
            }
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Health check task: logs queue statistics
     */
    @Scheduled(fixedDelay = 60000) // 1 minute
    public void logQueueStats() {
        try {
            long queueSize = redisQueueService.getQueueSize();
            long processingCount = redisQueueService.getProcessingCount();
            long failedCount = redisQueueService.getFailedCount();

            log.info("Queue Stats - Pending: {}, Processing: {}, Failed: {}, Total Processed: {}",
                    queueSize, processingCount, failedCount, processedCount.get());
        } catch (Exception e) {
            log.error("Error logging queue stats: {}", e.getMessage(), e);
        }
    }
}
