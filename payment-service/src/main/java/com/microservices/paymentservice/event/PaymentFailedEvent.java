package com.microservices.paymentservice.event;

import java.math.BigDecimal;

/**
 * Published by payment-service when payment processing fails.
 * Inventory-service listens and releases reserved inventory (compensation).
 * Order-service listens and cancels the order (compensation).
 */
public record PaymentFailedEvent(
        Long orderId,
        String productName,
        Integer quantity,
        BigDecimal amount,
        String reason,
        long timestamp) {
}
