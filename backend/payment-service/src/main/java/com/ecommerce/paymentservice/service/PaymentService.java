package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequestDto;
import com.ecommerce.paymentservice.dto.PaymentResponseDto;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private final Random random = new Random();

    public PaymentResponseDto processPayment(PaymentRequestDto paymentRequest) {
        String transactionId = UUID.randomUUID().toString();
        
        // Simulate payment processing delay
        try {
            Thread.sleep(1000 + random.nextInt(2000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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