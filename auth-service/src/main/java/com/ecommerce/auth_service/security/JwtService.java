package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class JwtService {

    private static final String SECRET =
            "mysecretkeymysecretkeymysecretkey123456";

    private final SecretKey key =
            Keys.hmacShaKeyFor(SECRET.getBytes());

    // TOKEN GENERATION
    public String generateToken(
            String username,
            Role role) {

        Map<String, Object> claims =
                new HashMap<>();

        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000 * 60 * 60
                        ))
                .signWith(key)
                .compact();
    }

    // EXTRACT ALL CLAIMS
    private Claims extractAllClaims(
            String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // EXTRACT USERNAME
    public String extractUsername(
            String token) {

        return extractAllClaims(token)
                .getSubject();
    }

    // EXTRACT ROLE
    public String extractRole(
            String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    // TOKEN VALIDATION
    public boolean isTokenValid(
            String token,
            String username) {

        String extractedUsername =
                extractUsername(token);

        return extractedUsername.equals(username)
                && !isTokenExpired(token);
    }

    // TOKEN EXPIRATION CHECK
    private boolean isTokenExpired(
            String token) {

        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }
}