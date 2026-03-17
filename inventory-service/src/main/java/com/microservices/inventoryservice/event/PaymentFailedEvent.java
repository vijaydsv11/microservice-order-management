package com.microservices.inventoryservice.event;

import java.math.BigDecimal;

/**
 * Published by payment-service when payment fails.
 * Inventory-service listens and releases the reserved stock (Saga compensation).
 */
public record PaymentFailedEvent(
        Long orderId,
        String productName,
        Integer quantity,
        BigDecimal amount,
        String reason,
        long timestamp) {
}
