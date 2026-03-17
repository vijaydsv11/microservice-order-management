package com.microservices.authservice.dto;

public record AuthResponse(
        String token,
        String username,
        String email,
        String message) {
}
