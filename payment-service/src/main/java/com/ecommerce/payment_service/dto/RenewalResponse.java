package com.ecommerce.payment_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenewalResponse {
    private Long id;
    private Long subscriptionId;
    private String razorpayPaymentId;
    private double amount;
    private String currency;
    private int cycleNumber;
    private LocalDateTime chargedAt;
}
