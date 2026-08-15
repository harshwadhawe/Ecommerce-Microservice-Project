package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequestDto;
import com.ecommerce.paymentservice.dto.PaymentResponseDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentRequestDto request(String orderId, BigDecimal amount) {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setOrderId(orderId);
        dto.setAmount(amount);
        dto.setPaymentMethod("CARD");
        dto.setCardNumber("4111111111111111");
        dto.setCvv("123");
        dto.setExpiryDate("12/30");
        dto.setCardholderName("A B");
        return dto;
    }

    @Test
    void succeedsWhenRandomIsBelowSuccessThreshold() {
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.5);
        PaymentService service = new PaymentService(random, 0);

        PaymentResponseDto response = service.processPayment(request("order-1", new BigDecimal("19.99")));

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("order-1", response.getOrderId());
        assertEquals(new BigDecimal("19.99"), response.getAmount());
        assertEquals("Payment processed successfully", response.getMessage());
    }

    @Test
    void failsWhenRandomIsAtOrAboveSuccessThreshold() {
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.95);
        when(random.nextInt(5)).thenReturn(1);
        PaymentService service = new PaymentService(random, 0);

        PaymentResponseDto response = service.processPayment(request("order-2", BigDecimal.TEN));

        assertEquals("FAILED", response.getStatus());
        assertEquals("Card declined", response.getMessage());
    }

    @Test
    void boundaryOfNinetyPercentIsAFailure() {
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.9);
        when(random.nextInt(5)).thenReturn(0);
        PaymentService service = new PaymentService(random, 0);

        assertEquals("FAILED", service.processPayment(request("order-3", BigDecimal.ONE)).getStatus());
    }

    @Test
    void everyPaymentGetsAUniqueTransactionId() {
        Random random = mock(Random.class);
        when(random.nextDouble()).thenReturn(0.1);
        PaymentService service = new PaymentService(random, 0);

        String first = service.processPayment(request("order-4", BigDecimal.ONE)).getTransactionId();
        String second = service.processPayment(request("order-4", BigDecimal.ONE)).getTransactionId();

        assertNotEquals(first, second);
        assertDoesNotThrow(() -> UUID.fromString(first));
    }
}
