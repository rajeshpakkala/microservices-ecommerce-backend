package com.ecommerce.notification_service.dto;

import lombok.Data;

@Data
public class UserInternalResponse {
    private String username;
    private String email;
    private String role;
}
