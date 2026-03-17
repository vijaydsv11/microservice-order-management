package com.microservices.inventoryservice.config;

import com.microservices.inventoryservice.entity.Inventory;
import com.microservices.inventoryservice.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialize inventory data on application startup
 */
@Component
@AllArgsConstructor
public class InitialDataLoader implements CommandLineRunner {

    private InventoryRepository inventoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (inventoryRepository.count() == 0) {
            // Create sample inventory items
            Inventory laptop = new Inventory();
            laptop.setProductName("Laptop");
            laptop.setQuantityAvailable(100);
            laptop.setQuantityReserved(0);
            laptop.setQuantityAllocated(0);
            inventoryRepository.save(laptop);

            Inventory mouse = new Inventory();
            mouse.setProductName("Mouse");
            mouse.setQuantityAvailable(500);
            mouse.setQuantityReserved(0);
            mouse.setQuantityAllocated(0);
            inventoryRepository.save(mouse);

            Inventory keyboard = new Inventory();
            keyboard.setProductName("Keyboard");
            keyboard.setQuantityAvailable(250);
            keyboard.setQuantityReserved(0);
            keyboard.setQuantityAllocated(0);
            inventoryRepository.save(keyboard);

            Inventory monitor = new Inventory();
            monitor.setProductName("Monitor");
            monitor.setQuantityAvailable(50);
            monitor.setQuantityReserved(0);
            monitor.setQuantityAllocated(0);
            inventoryRepository.save(monitor);

            Inventory headphones = new Inventory();
            headphones.setProductName("Headphones");
            headphones.setQuantityAvailable(200);
            headphones.setQuantityReserved(0);
            headphones.setQuantityAllocated(0);
            inventoryRepository.save(headphones);
        }
    }
}
