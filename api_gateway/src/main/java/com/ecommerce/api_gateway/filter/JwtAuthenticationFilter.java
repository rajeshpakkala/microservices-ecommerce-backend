package com.ecommerce.api_gateway.filter;

import com.ecommerce.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        // PUBLIC ROUTES
        if (path.startsWith(
                "/ecommerce/api/auth")) {

            return chain.filter(exchange);
        }

        String authHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION);

        // TOKEN MISSING
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.UNAUTHORIZED);

            return exchange.getResponse()
                    .setComplete();
        }

        String token =
                authHeader.substring(7);

        // TOKEN VALIDATION
        boolean valid =
                jwtUtil.validateToken(token);

        if (!valid) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.UNAUTHORIZED);

            return exchange.getResponse()
                    .setComplete();
        }

        Claims claims =
                jwtUtil.extractClaims(token);

        String username =
                claims.getSubject();

        String role =
                claims.get("role",
                        String.class);

        // FORWARD USER DETAILS
        ServerWebExchange modifiedExchange =
                exchange.mutate()
                        .request(r -> r
                                .header(
                                        "X-Auth-User",
                                        username)
                                .header(
                                        "X-Auth-Role",
                                        role))
                        .build();

        return chain.filter(modifiedExchange);
    }

    @Override
    public int getOrder() {

        return -1;
    }
}