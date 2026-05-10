package com.ecommerce.auth_service.dto;

import com.ecommerce.auth_service.enums.Role;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class VendorApprovalResponse {

    private Long userId;

    private String username;

    private String email;

    private Role role;

    private boolean approved;
}
