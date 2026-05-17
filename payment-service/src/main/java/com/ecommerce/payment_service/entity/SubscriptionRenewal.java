package com.ecommerce.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_renewals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRenewal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long subscriptionId;

    @Column(nullable = false)
    private String customerId;

    private String razorpayPaymentId;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String currency;

    // Which billing cycle this renewal is for
    private int cycleNumber;

    @CreationTimestamp
    private LocalDateTime chargedAt;
}
