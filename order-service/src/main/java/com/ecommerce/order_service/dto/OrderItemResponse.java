package com.ecommerce.order_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String vendorId;
    private Double price;
    private Integer quantity;
    private Double subtotal;
}
