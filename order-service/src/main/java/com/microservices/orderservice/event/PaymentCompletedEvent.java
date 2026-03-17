package com.microservices.orderservice.event;

import java.math.BigDecimal;

/**
 * Published by payment-service when payment succeeds.
 * Order-service listens and confirms the order (Saga Step 4 — happy path).
 */
public record PaymentCompletedEvent(
        Long orderId,
        Long paymentId,
        BigDecimal amount,
        long timestamp) {
}
