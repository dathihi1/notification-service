package com.badat.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_task")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    String recipient;
    String subject;
    int retryCount;

    public enum Status {PENDING, SENT, RETRY, FAILED}

    @Enumerated(EnumType.STRING)
    Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    String body;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
