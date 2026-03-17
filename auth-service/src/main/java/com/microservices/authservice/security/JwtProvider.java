package com.microservices.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;

@Component
public class JwtProvider {

    private static final String JWT_SECRET = "your-secret-key-change-this-in-production-your-secret-key-change-this-in-production";
    private static final long EXPIRATION_TIME = 86400000; // 24 hours
    private final SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .keyLocator(jwsHeader -> key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .keyLocator(jwsHeader -> key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

}
