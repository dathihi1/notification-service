package com.badat.notificationservice.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailMessage {
    String id;
    String recipient;
    String subject;
    String body;
    int retryCount;
    LocalDateTime createdAt;

    // Default constructor with UUID generation
    public EmailMessage(String recipient, String subject, String body) {
        this.id = java.util.UUID.randomUUID().toString();
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.retryCount = 0;
        this.createdAt = LocalDateTime.now();
    }
}