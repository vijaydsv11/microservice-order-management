package com.microservices.orderservice.config;

import com.microservices.orderservice.event.InventoryReservationFailedEvent;
import com.microservices.orderservice.event.PaymentCompletedEvent;
import com.microservices.orderservice.event.PaymentFailedEvent;
import com.microservices.orderservice.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Order-service Saga listeners — drives the order to its final state.
 *
 * <pre>
 *   InventoryReservationFailedEvent → CANCELLED  (step 2 compensation)
 *   PaymentCompletedEvent           → CONFIRMED  (step 4 happy path)
 *   PaymentFailedEvent              → CANCELLED  (step 4 compensation)
 * </pre>
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class StreamConfig {

    private OrderService orderService;

    /**
     * Saga step 2 failure — inventory could not be reserved; cancel immediately.
     */
    @Bean
    public Consumer<InventoryReservationFailedEvent> inventoryFailedConsumer() {
        return event -> {
            log.warn("Saga (inventory failed) — cancelling orderId={}, reason: {}",
                    event.orderId(), event.reason());
            orderService.cancelOrder(event.orderId(), event.reason());
        };
    }

    /** Saga step 4 success — payment processed; confirm the order. */
    @Bean
    public Consumer<PaymentCompletedEvent> paymentCompletedConsumer() {
        return event -> {
            log.info("Saga step 4 (success) — confirming orderId={}, paymentId={}",
                    event.orderId(), event.paymentId());
            orderService.confirmOrder(event.orderId());
        };
    }

    /**
     * Saga step 4 failure — payment declined; cancel and let inventory compensate.
     */
    @Bean
    public Consumer<PaymentFailedEvent> paymentFailedConsumer() {
        return event -> {
            log.warn("Saga step 4 (payment failed) — cancelling orderId={}, reason: {}",
                    event.orderId(), event.reason());
            orderService.cancelOrder(event.orderId(), event.reason());
        };
    }
}
