package com.microservices.inventoryservice.event;

import java.io.Serializable;

/**
 * OrderCreatedEvent record - Java 17 immutable event class
 * Consumed from Kafka topic "order-created-topic"
 */
public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String productName,
        Integer quantity,
        java.math.BigDecimal totalAmount,
        String userEmail,
        long timestamp
) implements Serializable {
}
