package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.entity.User;
import com.ecommerce.auth_service.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository repository,
                            PasswordEncoder passwordEncoder) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void createAdmin() {

        boolean adminExists =
                repository.findByUsername("admin").isPresent();

        if (!adminExists) {

            User admin = new User();

            admin.setUsername("admin");

            admin.setPassword(
                    passwordEncoder.encode("admin123"));

            admin.setRole("ADMIN");

            repository.save(admin);

            System.out.println("Default ADMIN created");
        }
    }
}
