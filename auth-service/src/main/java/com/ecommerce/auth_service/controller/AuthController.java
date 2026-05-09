package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.*;
import com.ecommerce.auth_service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ecommerce/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    // REGISTER
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                service.register(request));
    }

    // LOGIN -> SEND OTP
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(
            @RequestBody AuthRequest request) {

        return ResponseEntity.ok(
                service.login(request));
    }

    // VERIFY EMAIL
    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @RequestParam String token) {

        return ResponseEntity.ok(
                service.verifyEmail(token));
    }

    // VERIFY OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @RequestBody VerifyOtpRequest request) {

        return ResponseEntity.ok(
                service.verifyOtp(request));
    }
}