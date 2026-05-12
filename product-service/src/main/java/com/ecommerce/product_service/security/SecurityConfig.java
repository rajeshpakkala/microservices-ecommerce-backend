package com.ecommerce.product_service.security;

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

                // ---- INTERNAL SERVICE-TO-SERVICE (no auth needed) ----
                .requestMatchers("/ecommerce/api/products/internal/**")
                    .permitAll()

                // ---- VENDOR-SPECIFIC URL PATTERNS (specific before generic) ----
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/products/my-products")
                    .hasRole("VENDOR")
                .requestMatchers(HttpMethod.PATCH, "/ecommerce/api/products/*/stock")
                    .hasRole("VENDOR")

                // ---- ADMIN URL PATTERNS ----
                .requestMatchers("/ecommerce/api/products/admin/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/ecommerce/api/products/categories")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/ecommerce/api/products/categories/*")
                    .hasRole("ADMIN")

                // ---- PUBLIC BROWSING ----
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/products/fetch/all")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/products/search")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/products/category/*")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/products/vendor/*")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/products/categories/fetch/all")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/ecommerce/api/products/*")
                    .permitAll()

                // ---- VENDOR CRUD ----
                .requestMatchers(HttpMethod.POST, "/ecommerce/api/products")
                    .hasRole("VENDOR")
                .requestMatchers(HttpMethod.PUT, "/ecommerce/api/products/*")
                    .hasRole("VENDOR")
                .requestMatchers(HttpMethod.DELETE, "/ecommerce/api/products/*")
                    .hasRole("VENDOR")

                // ---- EVERYTHING ELSE ----
                .anyRequest().authenticated()
            )

            .addFilterBefore(headerAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
