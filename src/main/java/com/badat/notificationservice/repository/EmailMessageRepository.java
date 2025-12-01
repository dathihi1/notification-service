package com.badat.notificationservice.repository;

import com.badat.notificationservice.entity.EmailMessage;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for EmailMessage entity
 * For database operations on email messages
 */
@Repository
public interface EmailMessageRepository extends CrudRepository<EmailMessage, String> {

    /**
     * Find email messages by status
     */
    List<EmailMessage> findByStatus(String status);

    /**
     * Find email messages by recipient
     */
    List<EmailMessage> findByRecipient(String recipient);

    /**
     * Find email messages by status and recipient
     */
    List<EmailMessage> findByStatusAndRecipient(String status, String recipient);

    /**
     * Find email messages by priority
     */
    List<EmailMessage> findByPriority(String priority);

    /**
     * Find email messages by status ordered by creation time
     */
    List<EmailMessage> findByStatusOrderByCreatedAtAsc(String status);

    /**
     * Find email messages by status and priority ordered by creation time
     */
    List<EmailMessage> findByStatusAndPriorityOrderByCreatedAtAsc(String status, String priority);

    /**
     * Count email messages by status
     */
    long countByStatus(String status);

    /**
     * Find failed email messages with retry count less than max retries
     */
    @Query("SELECT e FROM EmailMessage e WHERE e.status = :status AND e.retryCount < e.maxRetries")
    List<EmailMessage> findByStatusAndRetryCountLessThanMaxRetries(@Param("status") String status);

    /**
     * Find old email messages for cleanup
     */
    List<EmailMessage> findByStatusInAndCreatedAtBefore(
            List<String> statuses,
            long cutoffTimestamp);

    /**
     * Delete email messages by IDs
     */
    void deleteByIdIn(List<String> ids);
}