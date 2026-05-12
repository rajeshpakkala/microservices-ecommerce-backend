package com.ecommerce.product_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponse<T> {

    private int responseCode;
    private String responseMessage;
    private boolean success;
    private T responseData;
}
