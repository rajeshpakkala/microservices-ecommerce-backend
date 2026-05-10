package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.*;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.enums.Role;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtService;
import jakarta.ws.rs.*;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    private final UserRepository repository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final MailSender mailSender;

    public AuthService(UserRepository repository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder, MailService mailService, MailSender mailSender) {

        this.repository = repository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.mailSender = mailSender;
    }

    // REGISTER
    public ApiResponse<AuthResponse> register(
            RegisterRequest request) {

        User user = new User();

        user.setUsername(request.getUsername());

        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()));

        String verificationToken =
                UUID.randomUUID().toString();

        user.setVerificationToken(verificationToken);

        user.setEmailVerified(false);

        user.setRole(Role.CUSTOMER);

        User savedUser = repository.save(user);

        // SEND EMAIL
        mailService.sendVerificationMail(
                savedUser.getEmail(),
                verificationToken);

        AuthResponse response =
                new AuthResponse(
                        savedUser.getUsername(),
                        savedUser.getRole(),
                        null
                );

        return new ApiResponse<>(
                200,
                "Verification mail sent successfully",
                true,
                response
        );
    }

    // LOGIN
    public ApiResponse<AuthResponse> login(
            AuthRequest request) {

        User user = repository.findByUsername(
                        request.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("User Not Found"));

        // VENDOR APPROVAL CHECK
        if (user.getRole() == Role.VENDOR
                && !user.isApproved()) {

            throw new RuntimeException(
                    "Vendor approval pending");
        }

        // EMAIL VERIFICATION CHECK
        if (user.getRole() != Role.ADMIN
                && !user.isEmailVerified()) {

            throw new RuntimeException(
                    "Please verify your email first");
        }

        // PASSWORD CHECK
        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword());

        if (!passwordMatches) {

            throw new RuntimeException(
                    "Invalid Password");
        }

        // ADMIN LOGIN -> DIRECT JWT
        if (user.getRole() == Role.ADMIN) {

            String token =
                    jwtService.generateToken(
                            user.getUsername(),
                            user.getRole());

            AuthResponse response =
                    new AuthResponse(
                            user.getUsername(),
                            user.getRole(),
                            token
                    );

            return new ApiResponse<>(
                    200,
                    "Admin Login Successful",
                    true,
                    response
            );
        }

        // CUSTOMER/VENDOR -> OTP FLOW

        // GENERATE OTP
        String otp = generateOtp();

        user.setOtp(otp);

        user.setOtpGeneratedTime(
                LocalDateTime.now());

        repository.save(user);

        // SEND OTP MAIL
        mailService.sendOtpMail(
                user.getEmail(),
                otp);

        return new ApiResponse<>(
                200,
                "OTP sent to your email",
                true,
                null
        );
    }


    private String generateOtp() {

        return String.valueOf(
                (int) ((Math.random() * 900000) + 100000));
    }

    public ApiResponse<String> verifyEmail(String token) {

        User user =
                repository.findByVerificationToken(token)
                        .orElseThrow(() ->
                                new RuntimeException("Invalid Token"));

        user.setEmailVerified(true);

        user.setVerificationToken(null);

        repository.save(user);

        return new ApiResponse<>(
                200,
                "Email Verified Successfully",
                true,
                "Account verified successfully.Please try to login"
        );
    }


    public ApiResponse<AuthResponse> verifyOtp(
            VerifyOtpRequest request) {

        User user = repository.findByUsername(
                        request.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("User Not Found"));

        // OTP CHECK
        if (!user.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        // OTP EXPIRATION CHECK
        if (user.getOtpGeneratedTime()
                .plusMinutes(5)
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException("OTP Expired");
        }

        // GENERATE JWT
        String token =
                jwtService.generateToken(
                        user.getUsername(),
                        user.getRole());

        // CLEAR OTP
        user.setOtp(null);

        user.setOtpGeneratedTime(null);

        repository.save(user);

        AuthResponse response =
                new AuthResponse(
                        user.getUsername(),
                        user.getRole(),
                        token
                );

        return new ApiResponse<>(
                200,
                "Login Successful",
                true,
                response
        );
    }

    public ApiResponse<AuthResponse>
    registerCustomer(RegisterRequest request) {

        User user = new User();

        user.setUsername(request.getUsername());

        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()));

        user.setRole(Role.CUSTOMER);

        user.setApproved(true);

        user.setEmailVerified(false);

        String token =
                UUID.randomUUID().toString();

        user.setVerificationToken(token);

        User savedUser = repository.save(user);

        mailService.sendVerificationMail(
                savedUser.getEmail(),
                token);

        AuthResponse response =
                new AuthResponse(
                        savedUser.getUsername(),
                        savedUser.getRole(),
                        null
                );

        return new ApiResponse<>(
                200,
                "Customer Registered Successfully",
                true,
                response
        );
    }

    public ApiResponse<AuthResponse>
    registerVendor(RegisterRequest request) {

        User user = new User();

        user.setUsername(request.getUsername());

        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()));

        user.setRole(Role.VENDOR);

        // WAITING FOR ADMIN APPROVAL
        user.setApproved(false);

        user.setEmailVerified(false);

        String token =
                UUID.randomUUID().toString();

        user.setVerificationToken(token);

        User savedUser = repository.save(user);

        mailService.sendVerificationMail(
                savedUser.getEmail(),
                token);

        AuthResponse response =
                new AuthResponse(
                        savedUser.getUsername(),
                        savedUser.getRole(),
                        null
                );

        return new ApiResponse<>(
                200,
                "Vendor Registration Submitted",
                true,
                response
        );
    }

    public ApiResponse<VendorApprovalResponse>
    approveVendor(Long id) {

        if (id == null) {

            throw new IllegalArgumentException(
                    "Id must be provided");
        }

        User vendor =
                repository.findById(id)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Vendor Not Found with id: " + id));

        if (vendor.getRole() != Role.VENDOR) {

            throw new IllegalArgumentException(
                    "User is not a vendor");
        }

        vendor.setApproved(true);

        User savedVendor =
                repository.save(vendor);

        VendorApprovalResponse response =
                VendorApprovalResponse.builder()
                        .userId(savedVendor.getId())
                        .username(savedVendor.getUsername())
                        .email(savedVendor.getEmail())
                        .role(savedVendor.getRole())
                        .approved(savedVendor.isApproved())
                        .build();

        return ApiResponse
                .<VendorApprovalResponse>builder()
                .responseCode(HttpStatus.OK.value())
                .responseMessage(
                        "Vendor Approved Successfully")
                .success(true)
                .responseData(response)
                .build();
    }
}