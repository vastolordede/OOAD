package com.ooad.cosmetics.dto.order;

import com.ooad.cosmetics.entity.Payment;
import com.ooad.cosmetics.entity.PaymentMethod;
import com.ooad.cosmetics.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String transactionRef,
        Instant paidAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getTransactionRef(),
                payment.getPaidAt()
        );
    }
}
