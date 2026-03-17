package com.microservices.inventoryservice.config;

import com.microservices.inventoryservice.event.InventoryReservationFailedEvent;
import com.microservices.inventoryservice.event.InventoryReservedEvent;
import com.microservices.inventoryservice.event.OrderCreatedEvent;
import com.microservices.inventoryservice.event.PaymentFailedEvent;
import com.microservices.inventoryservice.service.InventoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Inventory-service Saga listeners.
 *
 * <pre>
 *   Saga Step 2: OrderCreatedEvent     → reserve inventory
 *                → InventoryReservedEvent (success) | InventoryReservationFailedEvent (failure)
 *
 *   Saga Compensation: PaymentFailedEvent → release reserved inventory
 * </pre>
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class StreamConfig {

    private InventoryService inventoryService;
    private StreamBridge streamBridge;

    /** Saga Step 2 — reserve inventory after an order is created. */
    @Bean
    public Consumer<OrderCreatedEvent> orderCreatedConsumer() {
        return event -> {
            log.info("Saga step 2 — reserving inventory for orderId={}, product='{}', qty={}",
                    event.orderId(), event.productName(), event.quantity());
            try {
                inventoryService.handleOrderCreatedEvent(event);   // @Transactional — commits before we get here
                streamBridge.send("inventory-reserved-out-0",
                        new InventoryReservedEvent(event.orderId(), event.productName(),
                                event.quantity(), System.currentTimeMillis()));
                log.info("Saga step 2 — inventory reserved for orderId={}", event.orderId());
            } catch (Exception ex) {
                log.error("Saga step 2 — inventory reservation failed for orderId={}: {}",
                        event.orderId(), ex.getMessage());
                streamBridge.send("inventory-failed-out-0",
                        new InventoryReservationFailedEvent(event.orderId(), event.productName(),
                                event.quantity(), ex.getMessage(), System.currentTimeMillis()));
            }
        };
    }

    /** Saga compensation — release reserved inventory when payment fails. */
    @Bean
    public Consumer<PaymentFailedEvent> paymentFailedConsumer() {
        return event -> {
            log.warn("Saga compensation — releasing inventory for orderId={}, product='{}', qty={}",
                    event.orderId(), event.productName(), event.quantity());
            try {
                inventoryService.releaseReservedInventory(event.productName(), event.quantity());
                log.info("Saga compensation — inventory released for orderId={}", event.orderId());
            } catch (Exception ex) {
                log.error("Saga compensation — failed to release inventory for orderId={}: {}",
                        event.orderId(), ex.getMessage());
            }
        };
    }
}
