package com.ecommerce.api_gateway.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int responseCode;

    private String responseMessage;

    private boolean success;

    private T responseData;
}
