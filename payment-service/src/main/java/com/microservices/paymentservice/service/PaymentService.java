package com.microservices.paymentservice.service;

import com.microservices.paymentservice.entity.Payment;
import com.microservices.paymentservice.event.InventoryReservedEvent;
import com.microservices.paymentservice.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Saga Step 3 — processes payment for a reserved order.
 *
 * Creates a payment record and attempts to charge the customer.
 * Throws on failure so StreamConfig can publish the appropriate saga event.
 */
@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

    private PaymentRepository paymentRepository;

    /**
     * Atomically create and process a payment for the given inventory-reserved event.
     * Throws {@link RuntimeException} on any processing failure so the caller can
     * publish a {@code PaymentFailedEvent} and trigger saga compensation.
     *
     * @param event the InventoryReservedEvent that triggered this payment
     * @return the persisted {@link Payment} with status COMPLETED
     */
    @Transactional
    public Payment processPayment(InventoryReservedEvent event) {
        log.info("Processing payment for orderId={}, product='{}', qty={}",
                event.orderId(), event.productName(), event.quantity());

        // Idempotency guard — reject duplicate payment attempts
        paymentRepository.findByOrderId(event.orderId()).ifPresent(existing -> {
            throw new RuntimeException(
                    "Payment already processed for orderId=" + event.orderId()
                    + " with status=" + existing.getStatus());
        });

        BigDecimal amount = calculateAmount(event);

        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setAmount(amount);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        Payment saved = paymentRepository.save(payment);

        // Perform the actual charge (extend this with real payment gateway integration)
        charge(saved, amount);

        saved.setStatus(Payment.PaymentStatus.COMPLETED);
        Payment completed = paymentRepository.save(saved);

        log.info("Payment completed: paymentId={}, orderId={}, amount={}",
                completed.getId(), completed.getOrderId(), completed.getAmount());
        return completed;
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Placeholder for real payment-gateway integration.
     * Extend this method to call Stripe, PayPal, etc.
     */
    private void charge(Payment payment, BigDecimal amount) {
        // TODO: integrate with real payment gateway
        log.debug("Charging {} for paymentId={}", amount, payment.getId());
    }

    /** Simple unit-price × quantity calculation — replace with real pricing logic. */
    private BigDecimal calculateAmount(InventoryReservedEvent event) {
        // Placeholder: each unit costs 10.00
        return BigDecimal.TEN.multiply(BigDecimal.valueOf(event.quantity()));
    }
}
