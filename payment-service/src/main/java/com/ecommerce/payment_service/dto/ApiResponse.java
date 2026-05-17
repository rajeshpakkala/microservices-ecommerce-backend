package com.ecommerce.payment_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private int responseCode;
    private String responseMessage;
    private boolean success;
    private T responseData;
}
