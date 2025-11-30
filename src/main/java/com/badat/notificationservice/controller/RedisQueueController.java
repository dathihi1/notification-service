package com.badat.notificationservice.controller;

import com.badat.notificationservice.dto.ApiResponse;
import com.badat.notificationservice.dto.EmailMessage;
import com.badat.notificationservice.service.RedisEmailQueueService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/redis-queue")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RedisQueueController {

    RedisEmailQueueService redisQueueService;

    @PostMapping("/add-email")
    public ResponseEntity<ApiResponse<Object>> addEmailToQueue(@RequestBody Map<String, String> request) {
        try {
            String recipient = request.get("recipient");
            String subject = request.get("subject");
            String body = request.get("body");

            if (recipient == null || subject == null || body == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Object>builder()
                                .code(1001)
                                .message("Missing required fields: recipient, subject, body")
                                .build());
            }

            EmailMessage message = new EmailMessage(recipient, subject, body);
            redisQueueService.addToQueue(message);

            return ResponseEntity.ok()
                    .body(ApiResponse.<Object>builder()
                            .message("Email added to queue successfully")
                            .result(Map.of(
                                    "id", message.getId(),
                                    "recipient", recipient,
                                    "subject", subject
                            ))
                            .build());

        } catch (Exception e) {
            log.error("Error adding email to queue: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Object>builder()
                            .code(2001)
                            .message("Failed to add email to queue: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/add-multiple-emails")
    public ResponseEntity<ApiResponse<Object>> addMultipleEmails(@RequestParam(defaultValue = "10") int count) {
        try {
            Map<String, Object> result = new HashMap<>();

            for (int i = 0; i < count; i++) {
                String email = "test" + System.currentTimeMillis() + "_" + i + "@example.com";
                EmailMessage message = new EmailMessage(
                        email,
                        "Test Email " + (i + 1),
                        "This is test email #" + (i + 1) + " from Redis Queue"
                );
                redisQueueService.addToQueue(message);
            }

            result.put("addedCount", count);
            result.put("message", "Successfully added " + count + " emails to queue");

            return ResponseEntity.ok()
                    .body(ApiResponse.<Object>builder()
                            .message("Multiple emails added successfully")
                            .result(result)
                            .build());

        } catch (Exception e) {
            log.error("Error adding multiple emails: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Object>builder()
                            .code(2002)
                            .message("Failed to add multiple emails: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getQueueStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("pendingEmails", redisQueueService.getQueueSize());
            stats.put("processingEmails", redisQueueService.getProcessingCount());
            stats.put("failedEmails", redisQueueService.getFailedCount());
            stats.put("processingIds", redisQueueService.getProcessingIds());

            return ResponseEntity.ok()
                    .body(ApiResponse.<Object>builder()
                            .message("Queue statistics retrieved successfully")
                            .result(stats)
                            .build());

        } catch (Exception e) {
            log.error("Error getting queue stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Object>builder()
                            .code(2003)
                            .message("Failed to get queue stats: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/failed-emails")
    public ResponseEntity<ApiResponse<Object>> getFailedEmails() {
        try {
            List<EmailMessage> failedEmails = redisQueueService.getFailedEmails();

            return ResponseEntity.ok()
                    .body(ApiResponse.<Object>builder()
                            .message("Failed emails retrieved successfully")
                            .result(Map.of(
                                    "count", failedEmails.size(),
                                    "emails", failedEmails
                            ))
                            .build());

        } catch (Exception e) {
            log.error("Error getting failed emails: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Object>builder()
                            .code(2004)
                            .message("Failed to get failed emails: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/clear-processing")
    public ResponseEntity<ApiResponse<Object>> clearProcessingQueue() {
        try {
            redisQueueService.clearProcessing();

            return ResponseEntity.ok()
                    .body(ApiResponse.<Object>builder()
                            .message("Processing queue cleared successfully")
                            .result(Map.of("message", "All stuck emails have been cleared from processing"))
                            .build());

        } catch (Exception e) {
            log.error("Error clearing processing queue: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Object>builder()
                            .code(2005)
                            .message("Failed to clear processing queue: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("queueSize", redisQueueService.getQueueSize());
            health.put("processingCount", redisQueueService.getProcessingCount());
            health.put("failedCount", redisQueueService.getFailedCount());
            health.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok()
                    .body(ApiResponse.<Object>builder()
                            .message("Redis Queue service is healthy")
                            .result(health)
                            .build());

        } catch (Exception e) {
            log.error("Error in health check: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Object>builder()
                            .code(5000)
                            .message("Redis Queue service is unhealthy: " + e.getMessage())
                            .result(Map.of("status", "DOWN"))
                            .build());
        }
    }

    // Test endpoint to check if specific email is being processed
    @GetMapping("/is-processing/{emailId}")
    public ResponseEntity<ApiResponse<Object>> isEmailProcessing(@PathVariable String emailId) {
        try {
            boolean isProcessing = redisQueueService.isProcessing(emailId);

            return ResponseEntity.ok()
                    .body(ApiResponse.<Object>builder()
                            .message("Email processing status retrieved")
                            .result(Map.of(
                                    "emailId", emailId,
                                    "isProcessing", isProcessing
                            ))
                            .build());

        } catch (Exception e) {
            log.error("Error checking email processing status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.<Object>builder()
                            .code(2006)
                            .message("Failed to check email processing status: " + e.getMessage())
                            .build());
        }
    }
}