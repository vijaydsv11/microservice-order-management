package com.microservices.paymentservice.config;

import com.microservices.paymentservice.entity.Payment;
import com.microservices.paymentservice.event.InventoryReservedEvent;
import com.microservices.paymentservice.event.PaymentCompletedEvent;
import com.microservices.paymentservice.event.PaymentFailedEvent;
import com.microservices.paymentservice.service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Saga Step 3 — Payment processing.
 *
 * Listens for {@link InventoryReservedEvent} from inventory-service, attempts to
 * process payment, then publishes:
 * <ul>
 *   <li>{@link PaymentCompletedEvent} → order-service confirms the order (happy path)</li>
 *   <li>{@link PaymentFailedEvent}    → inventory-service releases stock AND
 *                                       order-service cancels the order (compensation)</li>
 * </ul>
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class StreamConfig {

    private PaymentService paymentService;
    private StreamBridge streamBridge;

    @Bean
    public Consumer<InventoryReservedEvent> inventoryReservedConsumer() {
        return event -> {
            log.info("Saga step 3 — processing payment for orderId={}", event.orderId());
            try {
                Payment payment = paymentService.processPayment(event);   // @Transactional
                streamBridge.send("payment-completed-out-0",
                        new PaymentCompletedEvent(
                                event.orderId(),
                                payment.getId(),
                                payment.getAmount(),
                                System.currentTimeMillis()));
                log.info("Saga step 3 — payment completed for orderId={}", event.orderId());
            } catch (Exception ex) {
                log.error("Saga step 3 — payment failed for orderId={}: {}", event.orderId(), ex.getMessage());
                streamBridge.send("payment-failed-out-0",
                        new PaymentFailedEvent(
                                event.orderId(),
                                event.productName(),
                                event.quantity(),
                                null,
                                ex.getMessage(),
                                System.currentTimeMillis()));
            }
        };
    }
}
