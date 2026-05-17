package com.ecommerce.payment_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateResponse {
    private Long paymentId;
    private String razorpayOrderId;
    private double amount;
    private String currency;
    private String keyId;
}
