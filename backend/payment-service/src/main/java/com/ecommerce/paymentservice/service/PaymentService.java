package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequestDto;
import com.ecommerce.paymentservice.dto.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private final Random random;
    private final long delayMs;

    @Autowired
    public PaymentService(@Value("${payment.simulated-delay-ms:1000}") long delayMs) {
        this(new Random(), delayMs);
    }

    // Tests inject a stubbed Random and delayMs=0 to make the simulator deterministic and fast.
    PaymentService(Random random, long delayMs) {
        this.random = random;
        this.delayMs = delayMs;
    }

    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest) {
        String transactionId = UUID.randomUUID().toString();

        // Simulate payment processing delay
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs + random.nextInt(2000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Simulate payment success/failure (90% success rate)
        boolean isSuccess = random.nextDouble() < 0.9;

        if (isSuccess) {
            return new PaymentResponseDto(
                transactionId,
                paymentRequest.getOrderId(),
                paymentRequest.getAmount(),
                "SUCCESS",
                "Payment processed successfully"
            );
        } else {
            String[] failureReasons = {
                "Insufficient funds",
                "Card declined",
                "Invalid card details",
                "Payment timeout",
                "Network error"
            };
            String failureReason = failureReasons[random.nextInt(failureReasons.length)];
            
            return new PaymentResponseDto(
                transactionId,
                paymentRequest.getOrderId(),
                paymentRequest.getAmount(),
                "FAILED",
                failureReason
            );
        }
    }
}