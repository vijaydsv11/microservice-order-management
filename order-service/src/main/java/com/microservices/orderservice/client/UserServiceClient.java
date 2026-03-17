package com.microservices.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "http://localhost:8082")
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    ResponseEntity<UserResponse> getUserById(@PathVariable("id") Long id);

    @GetMapping("/users/email/{email}")
    ResponseEntity<UserResponse> getUserByEmail(@PathVariable("email") String email);

    public class UserResponse {
        public Long id;
        public String firstName;
        public String lastName;
        public String email;
        public String phone;
        public String address;
        public String city;
        public String state;
        public String zipCode;
    }

}
