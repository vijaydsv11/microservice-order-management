package com.microservices.paymentservice.event;

/**
 * Consumed from inventory-service when inventory reservation succeeds.
 * Triggers payment processing (Saga Step 3).
 */
public record InventoryReservedEvent(
        Long orderId,
        String productName,
        Integer quantity,
        long timestamp) {
}
