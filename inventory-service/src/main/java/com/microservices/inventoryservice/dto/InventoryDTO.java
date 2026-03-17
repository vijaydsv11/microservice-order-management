package com.microservices.inventoryservice.dto;

/**
 * InventoryDTO record - Java 17 immutable data class
 */
public record InventoryDTO(
        Long id,
        String productName,
        Integer quantityAvailable,
        Integer quantityReserved,
        Integer quantityAllocated,
        Integer quantityFree
) {
}
