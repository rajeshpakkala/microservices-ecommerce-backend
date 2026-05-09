package com.ecommerce.auth_service.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationMail(
            String toEmail,
            String token) {

        String subject = "Verify Your Account";

        String body =
                "Click the link to verify your account:\n" +
                        "http://localhost:8082/ecommerce/api/auth/verify?token="
                        + token;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    public void sendOtpMail(
            String toEmail,
            String otp) {

        String subject = "Login OTP Verification";

        String body =
                "Your OTP is: " + otp +
                        "\nValid for 5 minutes.";

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        }
}
