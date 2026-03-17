package com.microservices.authservice.service;

import com.microservices.authservice.dto.LoginRequest;
import com.microservices.authservice.dto.RegisterRequest;
import com.microservices.authservice.dto.AuthResponse;
import com.microservices.authservice.entity.User;
import com.microservices.authservice.repository.UserRepository;
import com.microservices.authservice.security.JwtProvider;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service using Java 17 records and Stream API
 */
@Service
@AllArgsConstructor
public class AuthService {

    private UserRepository userRepository;
    private JwtProvider jwtProvider;
    private PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        // Use record accessors for field access
        if (userRepository.existsByUsername(request.username())) {
            return new AuthResponse(null, null, null, "Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            return new AuthResponse(null, null, null, "Email already exists");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        userRepository.save(user);

        String token = jwtProvider.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        // Use record accessors for field access
        User user = userRepository.findByUsername(request.username())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return new AuthResponse(null, null, null, "Invalid username or password");
        }

        String token = jwtProvider.generateToken(user.getUsername());

        return new AuthResponse(token, user.getUsername(), user.getEmail(), "Login successful");
    }

}
