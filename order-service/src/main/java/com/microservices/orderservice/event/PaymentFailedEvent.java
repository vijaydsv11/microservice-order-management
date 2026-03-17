package com.microservices.orderservice.event;

import java.math.BigDecimal;

/**
 * Published by payment-service when payment fails.
 * Order-service listens and cancels the order (Saga compensation).
 */
public record PaymentFailedEvent(
        Long orderId,
        String productName,
        Integer quantity,
        BigDecimal amount,
        String reason,
        long timestamp) {
}
