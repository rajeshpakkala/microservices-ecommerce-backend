package com.ecommerce.auth_service.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String username;

    private String otp;
}