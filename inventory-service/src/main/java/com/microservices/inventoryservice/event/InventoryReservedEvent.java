package com.microservices.inventoryservice.event;

import java.io.Serializable;

/**
 * Published by inventory-service after successfully reserving stock.
 * Consumed by order-service to confirm the order (Saga step 3 — success path).
 */
public record InventoryReservedEvent(
        Long orderId,
        String productName,
        Integer quantity,
        long timestamp) implements Serializable {
}
