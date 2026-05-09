package com.ecommerce.auth_service.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String username;

    private String role;
    private String token;
}
