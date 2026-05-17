package com.ecommerce.payment_service.dto;

import com.ecommerce.payment_service.enums.SubscriptionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {
    private Long id;
    private String customerId;
    private PlanResponse plan;
    private String razorpaySubscriptionId;
    private String authorizationUrl;    // customer visits this to authorize first payment + mandate
    private SubscriptionStatus status;
    private int paidCount;
    private int remainingCount;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime nextBillingAt;
    private LocalDateTime createdAt;
}
