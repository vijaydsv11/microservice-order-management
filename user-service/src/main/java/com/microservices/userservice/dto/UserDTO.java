package com.microservices.userservice.dto;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String city,
        String state,
        String zipCode) {
}
