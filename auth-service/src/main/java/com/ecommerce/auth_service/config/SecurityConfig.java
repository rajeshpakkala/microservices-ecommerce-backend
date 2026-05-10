package com.ecommerce.auth_service.config;

import com.ecommerce.auth_service.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter filter;

    public SecurityConfig(JwtAuthenticationFilter filter) {
        this.filter = filter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        // PUBLIC APIS
                        .requestMatchers(
                                "/ecommerce/api/auth/register/**","/ecommerce/api/auth/register/vendor",
                                "/ecommerce/api/auth/login",
                                "/ecommerce/api/auth/verify",
                                "/ecommerce/api/auth/verify-otp"
                        ).permitAll()

                        // ADMIN APIS
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        // USER APIS
                        .requestMatchers("/user/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers("/vendor/**")
                        .hasRole("VENDOR")

                        .requestMatchers("/customer/**")
                        .hasRole("CUSTOMER")

                        // EVERYTHING ELSE
                        .anyRequest()
                        .authenticated())

                .addFilterBefore(
                        filter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}