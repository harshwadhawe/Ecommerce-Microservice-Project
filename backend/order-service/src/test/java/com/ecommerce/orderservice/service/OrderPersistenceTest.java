package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CartDto;
import com.ecommerce.orderservice.dto.CartItemDto;
import com.ecommerce.orderservice.dto.CreateOrderDto;
import com.ecommerce.orderservice.dto.PaymentResponseDto;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.PaymentDeclinedException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Runs against a real transaction manager (H2), which the mocked-repository tests cannot do. It
 * exists because of a bug they could not catch: throwing PaymentDeclinedException from inside
 * @Transactional rolled the declined order straight back out of the database, so a failed payment
 * left no trace at all. Only the noRollbackFor rule keeps that record.
 */
@SpringBootTest
class OrderPersistenceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private CartClient cartClient;

    @MockBean
    private PaymentClient paymentClient;

    private CreateOrderDto request;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        request = new CreateOrderDto();
        request.setRecipientName("Ada Lovelace");
        request.setAddress("1 Main St");
        request.setCity("Chicago");
        request.setCountry("USA");
        request.setPostalCode("60601");
        request.setCardholderName("Ada Lovelace");
        request.setCardNumber("4111111111111111");
        request.setExpiryDate("12/30");
        request.setCvv("123");

        CartDto cart = new CartDto();
        cart.setUserId("7");
        cart.setItems(List.of(new CartItemDto("p1", "Laptop", new BigDecimal("999.99"), 2, null)));
        cart.setTotalAmount(new BigDecimal("1999.98"));
        cart.setTotalItems(2);
        when(cartClient.getCart(anyString(), anyString())).thenReturn(cart);
    }

    private PaymentResponseDto payment(String status, String message) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setStatus(status);
        response.setMessage(message);
        response.setTransactionId("txn-1");
        return response;
    }

    @Test
    void aDeclinedOrderSurvivesTheThrownException() {
        when(paymentClient.charge(any())).thenReturn(payment("FAILED", "Card declined"));

        assertThrows(PaymentDeclinedException.class,
                () -> orderService.placeOrder("7", "Bearer token", request));

        List<Order> stored = orderRepository.findAll();
        assertEquals(1, stored.size(), "the declined attempt must still be recorded");
        assertEquals(OrderStatus.PAYMENT_FAILED, stored.get(0).getStatus());
        assertEquals("Card declined", stored.get(0).getPaymentMessage());
        assertEquals(1, stored.get(0).getItems().size());
    }

    @Test
    void aPaidOrderIsStoredWithItsItems() {
        when(paymentClient.charge(any())).thenReturn(payment("SUCCESS", "Payment processed successfully"));

        Order placed = orderService.placeOrder("7", "Bearer token", request);

        Order stored = orderRepository.findById(placed.getId()).orElseThrow();
        assertEquals(OrderStatus.PAID, stored.getStatus());
        assertEquals("txn-1", stored.getPaymentTransactionId());
        assertEquals(1, stored.getItems().size());
        assertEquals(2, stored.getItems().get(0).getQuantity());
        assertEquals(0, new BigDecimal("1999.98").compareTo(stored.getTotalAmount()));
    }

    @Test
    void ordersAreListedForTheOwnerOnly() {
        when(paymentClient.charge(any())).thenReturn(payment("SUCCESS", "ok"));
        orderService.placeOrder("7", "Bearer token", request);
        orderService.placeOrder("8", "Bearer token", request);

        assertEquals(1, orderService.getOrdersForUser("7").size());
        assertEquals(1, orderService.getOrdersForUser("8").size());
    }
}
