package com.ecommerce.auth_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInternalResponse {
    private String username;
    private String email;
    private String role;
}
