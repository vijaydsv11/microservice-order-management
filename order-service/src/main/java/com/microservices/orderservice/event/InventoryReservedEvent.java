package com.microservices.orderservice.event;

/**
 * Published by inventory-service when inventory reservation succeeds.
 * Order-service listens and confirms the order (Saga step 3 — success path).
 */
public record InventoryReservedEvent(
        Long orderId,
        String productName,
        Integer quantity,
        long timestamp) {
}
