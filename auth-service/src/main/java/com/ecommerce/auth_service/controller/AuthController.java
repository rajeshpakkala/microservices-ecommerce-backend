package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.*;
import com.ecommerce.auth_service.service.AuthService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ecommerce/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    // REGISTER
    // CUSTOMER REGISTER
    @PostMapping("/register/customer")
    public ResponseEntity<ApiResponse<AuthResponse>>
    registerCustomer(
            @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                service.registerCustomer(request));
    }

    // VENDOR REGISTER
    @PostMapping("/register/vendor")
    public ResponseEntity<ApiResponse<AuthResponse>>
    registerVendor(
            @RequestBody RegisterRequest request) {

        return ResponseEntity.ok(
                service.registerVendor(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/approve-vendor/{id}")
    public ResponseEntity<
            ApiResponse<VendorApprovalResponse>>
    approveVendor(
            @PathVariable Long id) {

        ApiResponse<VendorApprovalResponse> response =
                service.approveVendor(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    // LOGIN -> SEND OTP
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
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