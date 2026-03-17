package com.microservices.orderservice.dto;

import java.math.BigDecimal;

public record OrderDTO(
        Long id,
        Long userId,
        String productName,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        String status,
        String userName,
        String userEmail) {
}
