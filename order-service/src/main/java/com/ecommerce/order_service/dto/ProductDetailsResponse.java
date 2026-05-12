package com.ecommerce.order_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDetailsResponse {

    private Long id;
    private String name;
    private Double price;
    private Integer stock;
    private String status;
    private String vendorId;
    private String categoryName;
}
