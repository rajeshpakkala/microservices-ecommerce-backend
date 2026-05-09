package com.ecommerce.auth_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private int responseCode;

    private String responseMessage;

    private boolean success;

    private T responseData;

    public ApiResponse(int i, String userRegisteredSuccessfully, boolean b) {
    }
}
