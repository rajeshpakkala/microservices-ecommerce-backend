package com.ecommerce.api_gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes());
    }

    // EXTRACT CLAIMS
    public Claims extractClaims(
            String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // VALIDATE TOKEN
    public boolean validateToken(
            String token) {

        try {

            extractClaims(token);

            return true;

        } catch (Exception ex) {

            return false;
        }
    }
}