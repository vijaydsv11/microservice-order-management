package com.microservices.orderservice.event;

/**
 * Published by inventory-service when inventory reservation fails.
 * Order-service listens and cancels the order (Saga step 3 — compensation
 * path).
 */
public record InventoryReservationFailedEvent(
        Long orderId,
        String productName,
        Integer quantity,
        String reason,
        long timestamp) {
}
