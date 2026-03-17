package com.microservices.inventoryservice.event;

import java.io.Serializable;

/**
 * Published by inventory-service when stock reservation fails (product not found or insufficient stock).
 * Consumed by order-service to cancel the order (Saga compensation transaction).
 */
public record InventoryReservationFailedEvent(
        Long orderId,
        String productName,
        Integer quantity,
        String reason,
        long timestamp) implements Serializable {
}
