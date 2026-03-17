package com.microservices.apigateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import javax.crypto.SecretKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Stream;

/**
 * JWT Authorization Filter using Spring Cloud Stream concepts
 * Validates JWT tokens on protected endpoints
 */
@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    @Value("${app.jwt.secret:your-secret-key-change-this-in-production}")
    private String jwtSecret;

    private SecretKey key;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/register",
            "/api-docs",
            "/swagger-ui",
            "/actuator/health"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Check if path is public (using Stream API)
        boolean isPublicPath = Stream.of(PUBLIC_PATHS)
                .anyMatch(path -> request.getRequestURI().startsWith(path));

        if (isPublicPath) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token != null && validateToken(token)) {
            String username = extractUsername(token);
            request.setAttribute("username", username);
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid or missing JWT token\"}");
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        return (header != null && header.startsWith(BEARER_PREFIX))
                ? header.substring(BEARER_PREFIX.length())
                : null;
    }

    private boolean validateToken(String token) {
        try {
            if (key == null) {
                key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            }
            Jwts.parser()
                    .keyLocator(jwsHeader -> key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            logger.warn("JWT validation failed: " + e.getMessage());
            return false;
        }
    }

    private String extractUsername(String token) {
        if (key == null) {
            key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        }
        return Jwts.parser()
                .keyLocator(jwsHeader -> key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

}
