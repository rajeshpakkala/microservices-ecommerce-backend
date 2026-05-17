package com.ecommerce.payment_service.dto;

import com.ecommerce.payment_service.enums.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String customerId;
    private double amount;
    private String currency;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private boolean clientVerified;
    private boolean webhookConfirmed;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
