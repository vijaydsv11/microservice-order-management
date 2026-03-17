package com.microservices.inventoryservice.controller;

import com.microservices.inventoryservice.dto.InventoryDTO;
import com.microservices.inventoryservice.service.InventoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@AllArgsConstructor
public class InventoryController {

    private InventoryService inventoryService;

    @GetMapping("/{id}")
    public ResponseEntity<InventoryDTO> getInventoryById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getInventoryById(id));
    }

    @GetMapping("/product/{productName}")
    public ResponseEntity<InventoryDTO> getInventoryByProductName(@PathVariable String productName) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductName(productName));
    }

    @GetMapping
    public ResponseEntity<List<InventoryDTO>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/low-stock/{threshold}")
    public ResponseEntity<List<InventoryDTO>> getLowStockItems(@PathVariable int threshold) {
        return ResponseEntity.ok(inventoryService.getLowStockItems(threshold));
    }

    @PostMapping("/add")
    public ResponseEntity<InventoryDTO> addInventory(
            @RequestParam String productName,
            @RequestParam Integer quantity) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryService.addInventory(productName, quantity));
    }

    @PostMapping("/reserve")
    public ResponseEntity<InventoryDTO> reserveInventory(
            @RequestParam String productName,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.reserveInventory(productName, quantity));
    }

    @PostMapping("/release")
    public ResponseEntity<InventoryDTO> releaseReservation(
            @RequestParam String productName,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(inventoryService.releaseReservation(productName, quantity));
    }
}
