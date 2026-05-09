package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.*;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.repository.UserRepository;
import com.ecommerce.auth_service.security.JwtService;
import org.springframework.mail.MailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

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

        user.setRole("CUSTOMER");

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
    public ApiResponse<String> login(
            AuthRequest request) {

        User user = repository.findByUsername(
                        request.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("User Not Found"));

        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "Please verify your email first");
        }

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword());

        if (!passwordMatches) {
            throw new RuntimeException("Invalid Password");
        }

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
                "Account verified successfully"
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
}