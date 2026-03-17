package com.microservices.orderservice.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount,
        String userEmail,
        long timestamp) {
}
