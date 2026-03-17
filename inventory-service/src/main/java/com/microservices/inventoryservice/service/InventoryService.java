package com.microservices.inventoryservice.service;

import com.microservices.inventoryservice.dto.InventoryDTO;
import com.microservices.inventoryservice.entity.Inventory;
import com.microservices.inventoryservice.event.OrderCreatedEvent;
import com.microservices.inventoryservice.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Inventory service with functional Stream API usage and Kafka event consumption
 */
@Service
@AllArgsConstructor
@Slf4j
public class InventoryService {

    private InventoryRepository inventoryRepository;

    /**
     * Saga step 2 — atomically reserve inventory for the incoming order.
     * Uses reserveQuantity() (soft lock) so the reservation can be released
     * if a later saga step requires compensation.
     * Throws on product-not-found or insufficient stock; the caller (StreamConfig)
     * catches and publishes the appropriate saga event.
     */
    @Transactional
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Reserving inventory: orderId={}, product='{}', qty={}",
                event.orderId(), event.productName(), event.quantity());

        Inventory inventory = inventoryRepository
                .findByProductName(event.productName())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found in inventory: " + event.productName()));

        inventory.reserveQuantity(event.quantity());
        inventoryRepository.save(inventory);

        log.info("Inventory reserved: product='{}', qty={}, freeAfter={}",
                event.productName(), event.quantity(), inventory.getQuantityFree());
    }

    /**
     * Saga compensation — releases previously reserved inventory when payment fails.
     * Throws on product-not-found so StreamConfig can log the anomaly.
     */
    @Transactional
    public void releaseReservedInventory(String productName, Integer quantity) {
        log.info("Releasing reserved inventory: product='{}', qty={}", productName, quantity);

        Inventory inventory = inventoryRepository
                .findByProductName(productName)
                .orElseThrow(() -> new RuntimeException(
                        "Cannot release — product not found: " + productName));

        inventory.releaseReservedQuantity(quantity);
        inventoryRepository.save(inventory);

        log.info("Inventory released: product='{}', qty={}, freeAfter={}",
                productName, quantity, inventory.getQuantityFree());
    }

    public InventoryDTO getInventoryByProductName(String productName) {
        Inventory inventory = inventoryRepository
                .findByProductName(productName)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToDTO(inventory);
    }

    public InventoryDTO getInventoryById(Long id) {
        Inventory inventory = inventoryRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));
        return convertToDTO(inventory);
    }

    public List<InventoryDTO> getAllInventory() {
        // Use Stream API with functional programming
        return inventoryRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<InventoryDTO> getLowStockItems(int threshold) {
        // Use Stream API to filter and map
        return inventoryRepository.findAll()
                .stream()
                .filter(inv -> inv.getQuantityFree() < threshold)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public InventoryDTO addInventory(String productName, Integer quantity) {
        Inventory inventory = inventoryRepository
                .findByProductName(productName)
                .orElseGet(() -> {
                    Inventory newInv = new Inventory();
                    newInv.setProductName(productName);
                    newInv.setQuantityAvailable(0);
                    newInv.setQuantityReserved(0);
                    newInv.setQuantityAllocated(0);
                    return newInv;
                });

        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);
        Inventory saved = inventoryRepository.save(inventory);
        return convertToDTO(saved);
    }

    public InventoryDTO reserveInventory(String productName, Integer quantity) {
        Inventory inventory = inventoryRepository
                .findByProductName(productName)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        inventory.reserveQuantity(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        return convertToDTO(saved);
    }

    public InventoryDTO releaseReservation(String productName, Integer quantity) {
        Inventory inventory = inventoryRepository
                .findByProductName(productName)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        inventory.releaseReservedQuantity(quantity);
        Inventory saved = inventoryRepository.save(inventory);
        return convertToDTO(saved);
    }

    private InventoryDTO convertToDTO(Inventory inventory) {
        return new InventoryDTO(
                inventory.getId(),
                inventory.getProductName(),
                inventory.getQuantityAvailable(),
                inventory.getQuantityReserved(),
                inventory.getQuantityAllocated(),
                inventory.getQuantityFree()
        );
    }
}
