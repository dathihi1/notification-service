package com.badat.notificationservice.controller;

import com.badat.notificationservice.dto.ApiResponse;
import com.badat.notificationservice.dto.request.NotificationRequest;
import com.badat.notificationservice.dto.response.NotificationResponse;
import com.badat.notificationservice.entity.NotificationMessage;
import com.badat.notificationservice.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Multi-Channel Notification Service
 * Provides endpoints for external services and internal management
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Receive notification from external service
     * Primary endpoint for other services to send notifications
     */
    @PostMapping
    public ApiResponse<NotificationResponse> receiveNotification(@RequestBody NotificationRequest request) {
            log.info("📥 Received notification request: {} -> {} via {}",
                    request.getRecipientType(), request.getRecipient(), request.getChannelType());

            NotificationResponse response = notificationService.receiveNotification(request);

            return ApiResponse.<NotificationResponse>builder()
                    .result(response)
                    .build();
        }

    /**
     * Get notifications by status
     */
    @GetMapping("/status/{status}")
    public ApiResponse<List<NotificationMessage>> getNotificationsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "10") int limit) {


        List<NotificationMessage> notifications = notificationService.getNotificationsByStatus(status, limit);
        return ApiResponse.<List<NotificationMessage>>builder()
                .result(notifications)
                .build();
    }

    /**
     * Get notifications by recipient
     */
    @GetMapping("/recipient/{recipient}")
    public ApiResponse<List<NotificationMessage>> getNotificationsByRecipient(
            @PathVariable String recipient,
            @RequestParam(defaultValue = "10") int limit) {

        List<NotificationMessage> notifications = notificationService.getNotificationsByRecipient(recipient, limit);
        return ApiResponse.<List<NotificationMessage>>builder()
                .result(notifications)
                .build();
    }

    /**
     * Get notifications by channel type
     */
    @GetMapping("/channel/{channelType}")
    public ApiResponse<List<NotificationMessage>> getNotificationsByChannel(
            @PathVariable String channelType,
            @RequestParam(defaultValue = "10") int limit) {

            List<NotificationMessage> notifications = notificationService.getNotificationsByChannelType(channelType, limit);
            return ApiResponse.<List<NotificationMessage>>builder()
                    .result(notifications)
                    .build();
    }

    /**
     * Health check for notification service
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> healthCheck() {
        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of(
                                "status", "UP",
                                "message", "Multi-Channel Notification Service is running",
                                "timestamp", System.currentTimeMillis(),
                                "version", "1.0.0",
                                "channels", java.util.List.of("EMAIL", "SMS", "PUSH_NOTIFICATION", "WEBHOOK", "IN_APP"),
                                "endpoints", java.util.List.of(
                                        "POST /api/notifications - Receive notifications",
                                        "GET /api/notifications/status/{status} - Get by status",
                                        "GET /api/notifications/recipient/{recipient} - Get by recipient",
                                        "GET /api/notifications/channel/{channelType} - Get by channel",
                                        "GET /api/notifications/health - Health check"
                                )))
                .build();
    }

    /**
     * Get notification statistics
     */
    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        // Calculate statistics
        long totalSent = getTotalSentCount();
        long totalFailed = getTotalFailedCount();
        long totalPending = getTotalPendingCount();

        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of(
                            "totalSent", totalSent,
                            "totalFailed", totalFailed,
                            "totalPending", totalPending,
                            "successRate", totalSent > 0 ? (double) totalSent / (totalSent + totalFailed) * 100 : 0.0,
                            "timestamp", System.currentTimeMillis()
                    ))
                    .build();
    }

    /**
     * Cleanup old notifications manually
     */
    @PostMapping("/cleanup")
    public ApiResponse<Void> cleanupNotifications() {
            notificationService.cleanupOldNotifications();
            return ApiResponse.<Void>builder().build();
    }

    // Helper methods for statistics
    private long getTotalSentCount() {
        // Implementation would query database
        return 1000; // Mock count
    }

    private long getTotalFailedCount() {
        return 100; // Mock count
    }

    private long getTotalPendingCount() {
        return 500; // Mock count
    }
}