package com.badat.notificationservice.repository;

import com.badat.notificationservice.entity.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification Repository with optimized queries for multi-channel support
 * Provides database access for NotificationMessage entities
 * Follows Spring Data JPA best practices with custom queries
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationMessage, String> {

    /**
     * Find pending notifications ordered by priority and creation time
     * For processing with @Scheduled worker
     */
    @Query("SELECT n FROM NotificationMessage n " +
                  "WHERE n.status = :status " +
                  "ORDER BY CASE n.priority " +
                  "  WHEN 'HIGH' THEN 1 " +
                  "  WHEN 'NORMAL' THEN 2 " +
                  "  WHEN 'LOW' THEN 3 " +
                  "  ELSE 2 END, " +
                  "n.createdAt ASC " +
                  "LIMIT :limit")
    List<NotificationMessage> findPendingNotificationsOrderedByPriority(
            @Param("status") NotificationMessage.NotificationStatus status,
            @Param("limit") int limit
    );

    /**
     * Find notifications by status with pagination
     */
    @Query("SELECT n FROM NotificationMessage n WHERE n.status = :status ORDER BY n.createdAt DESC LIMIT :limit")
    List<NotificationMessage> findByStatusOrderByCreatedAtDesc(
            @Param("status") String status,
            @Param("limit") int limit
    );

    /**
     * Find notifications by recipient type and channel
     */
    @Query("SELECT n FROM NotificationMessage n " +
            "WHERE n.recipientType = :recipientType " +
            "AND n.channelType = :channelType " +
            "AND n.status = :status " +
            "ORDER BY n.createdAt DESC " +
            "LIMIT :limit")
    List<NotificationMessage> findByRecipientTypeAndChannelTypeAndStatusOrderByCreatedAtDesc(
            @Param("recipientType") String recipientType,
            @Param("channelType") String channelType,
            @Param("status") String status,
            @Param("limit") int limit
    );

    /**
     * Find notifications by recipient
     */
    List<NotificationMessage> findByRecipientOrderByCreatedAtDesc(String recipient);

    /**
     * Find failed notifications for retry
     */
    @Query("SELECT n FROM NotificationMessage n " +
            "WHERE n.status = :status " +
            "AND n.retryCount < :maxRetries " +
            "AND n.createdAt < :cutoff " +
            "ORDER BY n.createdAt ASC")
    List<NotificationMessage> findFailedNotificationsForRetry(
            @Param("status") String status,
            @Param("maxRetries") Integer maxRetries,
            @Param("cutoff") LocalDateTime cutoff
    );

    /**
     * Count notifications by status for statistics
     */
    long countByStatus(String status);

    /**
     * Find notifications older than specified time for cleanup
     */
    @Query("SELECT n FROM NotificationMessage n " +
            "WHERE n.createdAt < :cutoff " +
            "AND n.status IN :statuses " +
            "ORDER BY n.createdAt ASC " +
            "LIMIT :limit")
    List<NotificationMessage> findOldNotificationsForCleanup(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("statuses") List<NotificationMessage.NotificationStatus> statuses,
            @Param("limit") int limit
    );

    /**
     * Delete old notifications in batch
     */
    void deleteByIdIn(List<String> ids);

    List<NotificationMessage> findByChannelTypeOrderByCreatedAtDesc(String channelType, int limit);
}