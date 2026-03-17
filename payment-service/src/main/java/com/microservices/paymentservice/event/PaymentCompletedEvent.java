package com.microservices.paymentservice.event;

import java.math.BigDecimal;

/**
 * Published by payment-service on successful payment.
 * Order-service listens and confirms the order (Saga Step 4 — success path).
 */
public record PaymentCompletedEvent(
        Long orderId,
        Long paymentId,
        BigDecimal amount,
        long timestamp) {
}
