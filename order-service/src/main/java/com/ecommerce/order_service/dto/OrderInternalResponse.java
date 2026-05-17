package com.ecommerce.order_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderInternalResponse {
    private Long id;
    private String customerId;
    private double totalAmount;
    private String status;
}
