package com.microservices.orderservice.producer;

import com.microservices.orderservice.event.OrderCreatedEvent;
import lombok.AllArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

/**
 * Event producer using Spring Cloud Stream
 */
@Service
@AllArgsConstructor
public class OrderCreatedEventProducer {

    private final StreamBridge streamBridge;

    /**
     * Publishes order created event to Kafka topic
     */
    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        streamBridge.send("order-created-out-0", event);
    }

}
