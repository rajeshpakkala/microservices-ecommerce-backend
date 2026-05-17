package com.ecommerce.notification_service.entity;

import com.ecommerce.notification_service.enums.NotificationStatus;
import com.ecommerce.notification_service.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientUsername;

    @Column(nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    // Stores the reference ID (orderId, paymentId, subscriptionId)
    private String referenceId;

    private String errorMessage;

    @CreationTimestamp
    private LocalDateTime sentAt;
}
