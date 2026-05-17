package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.UserInternalResponse;
import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/ecommerce/api/auth/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/users/{username}")
    public ResponseEntity<UserInternalResponse> getUserByUsername(@PathVariable String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + username));

        return ResponseEntity.ok(UserInternalResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build());
    }
}
