package com.ecommerce.payment_service.dto;

import lombok.Data;

@Data
public class PaymentInitiateRequest {
    private Long orderId;
}
