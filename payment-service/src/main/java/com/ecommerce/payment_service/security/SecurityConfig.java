package com.ecommerce.payment_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    public SecurityConfig(HeaderAuthenticationFilter headerAuthenticationFilter) {
        this.headerAuthenticationFilter = headerAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth

                // Razorpay webhook — no auth (Razorpay server calls this)
                .requestMatchers(HttpMethod.POST, "/ecommerce/api/payments/webhook")
                    .permitAll()

                // Public plan browsing — no auth needed
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/subscriptions/plans")
                    .permitAll()

                // Customer operations
                .requestMatchers(HttpMethod.POST, "/ecommerce/api/payments/initiate")
                    .hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/ecommerce/api/payments/verify")
                    .hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/payments/order/**")
                    .hasAnyRole("CUSTOMER", "ADMIN")

                .anyRequest().authenticated()
            )

            .addFilterBefore(headerAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
