package com.ecommerce.product_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductInternalResponse {

    private Long id;
    private String name;
    private Double price;
    private Integer stock;
    private String status;
    private String vendorId;
    private String categoryName;
}
