package com.microservices.userservice.service;

import com.microservices.userservice.dto.UserDTO;
import com.microservices.userservice.entity.User;
import com.microservices.userservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User service with functional Stream API usage
 */
@Service
@AllArgsConstructor
public class UserService {

    private UserRepository userRepository;

    public UserDTO createUser(UserDTO userDTO) {
        if (userRepository.existsByEmail(userDTO.email())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFirstName(userDTO.firstName());
        user.setLastName(userDTO.lastName());
        user.setEmail(userDTO.email());
        user.setPhone(userDTO.phone());
        user.setAddress(userDTO.address());
        user.setCity(userDTO.city());
        user.setState(userDTO.state());
        user.setZipCode(userDTO.zipCode());

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    public List<UserDTO> getAllUsers() {
        // Use Stream API for functional programming
        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(userDTO.firstName());
        user.setLastName(userDTO.lastName());
        user.setPhone(userDTO.phone());
        user.setAddress(userDTO.address());
        user.setCity(userDTO.city());
        user.setState(userDTO.state());
        user.setZipCode(userDTO.zipCode());

        User updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getCity(),
                user.getState(),
                user.getZipCode());
    }

}
