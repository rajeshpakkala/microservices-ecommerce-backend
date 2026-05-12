package com.ecommerce.order_service.security;

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

                // ---- ADMIN URL PATTERNS ----
                .requestMatchers("/ecommerce/api/orders/admin/**")
                    .hasRole("ADMIN")

                // ---- VENDOR URL PATTERNS ----
                .requestMatchers("/ecommerce/api/orders/vendor/**")
                    .hasRole("VENDOR")
                .requestMatchers(HttpMethod.PUT, "/ecommerce/api/orders/*/confirm")
                    .hasRole("VENDOR")
                .requestMatchers(HttpMethod.PUT, "/ecommerce/api/orders/*/ship")
                    .hasRole("VENDOR")

                // ---- CUSTOMER URL PATTERNS ----
                .requestMatchers(HttpMethod.POST, "/ecommerce/api/orders")
                    .hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/orders/my-orders")
                    .hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/orders/*")
                    .hasAnyRole("CUSTOMER", "VENDOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/ecommerce/api/orders/*/cancel")
                    .hasRole("CUSTOMER")

                // ---- EVERYTHING ELSE ----
                .anyRequest().authenticated()
            )

            .addFilterBefore(headerAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
