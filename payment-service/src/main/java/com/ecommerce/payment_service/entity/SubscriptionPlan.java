package com.ecommerce.payment_service.entity;

import com.ecommerce.payment_service.enums.PlanInterval;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanInterval interval;

    // Razorpay plan ID — created once and reused for all subscribers
    @Column(unique = true)
    private String razorpayPlanId;

    // How many billing cycles this plan runs (e.g. 12 for monthly = 1 year)
    @Column(nullable = false)
    private int totalBillingCycles;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
