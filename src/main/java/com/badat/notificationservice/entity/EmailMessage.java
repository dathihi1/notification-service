package com.badat.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Simple Email Message Entity
 * For Redis queue operations and lightweight notifications
 */
@Entity
@Table(name = "email_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Recipient email address
     */
    @Column(nullable = false, length = 255)
    private String recipient;

    /**
     * Email subject
     */
    @Column(nullable = false, length = 200)
    private String subject;

    /**
     * Email content
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * Carbon copy recipients (optional)
     */
    @Column
    private String[] cc;

    /**
     * Blind carbon copy recipients (optional)
     */
    @Column
    private String[] bcc;

    /**
     * Reply-to address (optional)
     */
    @Column
    private String replyTo;

    /**
     * Email template identifier (optional)
     */
    @Column(length = 100)
    private String templateId;

    /**
     * Email priority level
     */
    @Column
    private String priority;

    /**
     * Creation timestamp
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Sent timestamp (when email was sent)
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Retry counter for failed emails
     */
    private Integer retryCount;

    /**
     * Maximum retry attempts allowed
     */
    @Column
    private Integer maxRetries;

    /**
     * Error message if send failed
     */
    @Column(length = 1000)
    private String error;

    /**
     * Email status
     */
    @Column
    private String status;

    /**
     * Additional metadata for email (JSON format)
     */
    @Column(length = 2000)
    private String metadata;

    /**
     * Check if email can be retried
     */
    @Transient
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount != null ? this.retryCount : 0) + 1;
    }

    /**
     * Mark email as sent successfully
     */
    public void markAsSent() {
        this.status = "SENT";
        this.sentAt = java.time.LocalDateTime.now();
        this.error = null;
    }

    /**
     * Mark email as failed
     */
    public void markAsFailed(String errorMessage) {
        this.status = "FAILED";
        this.error = errorMessage;
        incrementRetryCount();
    }

    /**
     * Mark email as processing
     */
    public void markAsProcessing() {
        this.status = "PROCESSING";
    }

    /**
     * Check if email is pending
     */
    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    /**
     * Check if email is processing
     */
    public boolean isProcessing() {
        return "PROCESSING".equals(this.status);
    }

    /**
     * Check if email is sent
     */
    public boolean isSent() {
        return "SENT".equals(this.status);
    }

    /**
     * Check if email is failed
     */
    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }
}