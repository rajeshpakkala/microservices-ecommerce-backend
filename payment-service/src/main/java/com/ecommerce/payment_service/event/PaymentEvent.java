package com.ecommerce.payment_service.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {
    private Long orderId;
    private String customerId;
    private Long paymentId;
    private double amount;
    private String eventType;   // PAYMENT_SUCCESS | PAYMENT_FAILED
}
