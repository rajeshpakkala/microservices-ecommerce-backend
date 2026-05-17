package com.ecommerce.payment_service.dto;

import lombok.Data;

@Data
public class OrderDetailsResponse {
    private Long id;
    private String customerId;
    private double totalAmount;
    private String status;
}
