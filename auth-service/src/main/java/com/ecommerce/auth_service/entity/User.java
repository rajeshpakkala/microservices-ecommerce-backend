package com.ecommerce.auth_service.entity;

import com.ecommerce.auth_service.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean emailVerified;

    private String verificationToken;
    private String otp;
    private boolean approved;
    private LocalDateTime otpGeneratedTime;
}