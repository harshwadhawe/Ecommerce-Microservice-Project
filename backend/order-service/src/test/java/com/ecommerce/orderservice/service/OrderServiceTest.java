package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CartDto;
import com.ecommerce.orderservice.dto.CartItemDto;
import com.ecommerce.orderservice.dto.CreateOrderDto;
import com.ecommerce.orderservice.dto.PaymentRequestDto;
import com.ecommerce.orderservice.dto.PaymentResponseDto;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.DownstreamUnavailableException;
import com.ecommerce.orderservice.exception.EmptyCartException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.PaymentDeclinedException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String TOKEN = "Bearer a.b.c";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartClient cartClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderDto request;

    @BeforeEach
    void setUp() {
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
    }

    private CartDto cartWithTwoItems() {
        CartDto cart = new CartDto();
        cart.setUserId("7");
        cart.setItems(List.of(
                new CartItemDto("p1", "Laptop", new BigDecimal("999.99"), 1, "laptop.png"),
                new CartItemDto("p2", "Mouse", new BigDecimal("49.99"), 2, null)));
        cart.setTotalAmount(new BigDecimal("1099.97"));
        cart.setTotalItems(3);
        return cart;
    }

    private PaymentResponseDto payment(String status, String message) {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setStatus(status);
        response.setMessage(message);
        response.setTransactionId("txn-1");
        return response;
    }

    private void repositoryEchoesSaves() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void successfulCheckoutSnapshotsTheCartAndMarksTheOrderPaid() {
        when(cartClient.getCart("7", TOKEN)).thenReturn(cartWithTwoItems());
        when(paymentClient.charge(any())).thenReturn(payment("SUCCESS", "Payment processed successfully"));
        repositoryEchoesSaves();

        Order order = orderService.placeOrder("7", TOKEN, request);

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals("txn-1", order.getPaymentTransactionId());
        assertEquals(2, order.getItems().size());
        assertEquals(3, order.getTotalItems());
        assertEquals(0, new BigDecimal("1099.97").compareTo(order.getTotalAmount()));
        assertEquals("Laptop", order.getItems().get(0).getProductName());
        assertEquals("Ada Lovelace", order.getRecipientName());
        assertTrue(order.getOrderNumber().startsWith("ORD-"));
    }

    @Test
    void totalIsComputedFromCartItemsNotTakenFromTheClient() {
        CartDto cart = cartWithTwoItems();
        cart.setTotalAmount(new BigDecimal("0.01")); // a lying cart total must not become the charge
        when(cartClient.getCart("7", TOKEN)).thenReturn(cart);
        when(paymentClient.charge(any())).thenReturn(payment("SUCCESS", "ok"));
        repositoryEchoesSaves();

        orderService.placeOrder("7", TOKEN, request);

        ArgumentCaptor<PaymentRequestDto> charged = ArgumentCaptor.forClass(PaymentRequestDto.class);
        verify(paymentClient).charge(charged.capture());
        assertEquals(0, new BigDecimal("1099.97").compareTo(charged.getValue().getAmount()));
    }

    @Test
    void cartIsClearedOnlyAfterPaymentSucceeds() {
        when(cartClient.getCart("7", TOKEN)).thenReturn(cartWithTwoItems());
        when(paymentClient.charge(any())).thenReturn(payment("SUCCESS", "ok"));
        repositoryEchoesSaves();

        orderService.placeOrder("7", TOKEN, request);

        InOrder ordered = inOrder(paymentClient, cartClient);
        ordered.verify(paymentClient).charge(any());
        ordered.verify(cartClient).clearCart("7", TOKEN);
    }

    @Test
    void declinedPaymentKeepsTheCartAndRecordsAFailedOrder() {
        when(cartClient.getCart("7", TOKEN)).thenReturn(cartWithTwoItems());
        when(paymentClient.charge(any())).thenReturn(payment("FAILED", "Card declined"));
        repositoryEchoesSaves();

        PaymentDeclinedException thrown = assertThrows(PaymentDeclinedException.class,
                () -> orderService.placeOrder("7", TOKEN, request));

        assertEquals("Card declined", thrown.getMessage());
        assertEquals(OrderStatus.PAYMENT_FAILED, thrown.getOrder().getStatus());
        verify(cartClient, never()).clearCart(anyString(), anyString());
    }

    @Test
    void theAttemptIsPersistedBeforeThePaymentIsMade() {
        when(cartClient.getCart("7", TOKEN)).thenReturn(cartWithTwoItems());
        when(paymentClient.charge(any())).thenReturn(payment("SUCCESS", "ok"));
        repositoryEchoesSaves();

        orderService.placeOrder("7", TOKEN, request);

        // Saved as PENDING first, so a crash mid-charge still leaves a record of the attempt.
        InOrder ordered = inOrder(orderRepository, paymentClient);
        ordered.verify(orderRepository).save(any(Order.class));
        ordered.verify(paymentClient).charge(any());
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void emptyCartIsRejectedBeforeAnyCharge() {
        CartDto empty = new CartDto();
        empty.setItems(List.of());
        when(cartClient.getCart("7", TOKEN)).thenReturn(empty);

        assertThrows(EmptyCartException.class, () -> orderService.placeOrder("7", TOKEN, request));

        verify(paymentClient, never()).charge(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void anUnreachableCartServiceDoesNotProduceAnOrder() {
        when(cartClient.getCart("7", TOKEN))
                .thenThrow(new DownstreamUnavailableException("cart-service could not be reached", null));

        assertThrows(DownstreamUnavailableException.class, () -> orderService.placeOrder("7", TOKEN, request));

        verify(paymentClient, never()).charge(any());
    }

    @Test
    void aPaidOrderSurvivesAFailureToClearTheCart() {
        when(cartClient.getCart("7", TOKEN)).thenReturn(cartWithTwoItems());
        when(paymentClient.charge(any())).thenReturn(payment("SUCCESS", "ok"));
        repositoryEchoesSaves();
        doThrow(new DownstreamUnavailableException("cart-service could not be reached", null))
                .when(cartClient).clearCart("7", TOKEN);

        Order order = orderService.placeOrder("7", TOKEN, request);

        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void ordersAreListedNewestFirstForTheOwner() {
        when(orderRepository.findByUserIdOrderByCreatedAtDesc("7")).thenReturn(List.of(new Order()));

        assertEquals(1, orderService.getOrdersForUser("7").size());
    }

    @Test
    void anotherUsersOrderIsForbiddenNotFound() {
        Order other = new Order();
        other.setUserId("8");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class, () -> orderService.getOrderForUser(1L, "7"));
    }

    @Test
    void missingOrderIsNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderForUser(99L, "7"));
    }

    @Test
    void statusUpdateIsOwnerChecked() {
        Order other = new Order();
        other.setUserId("8");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class,
                () -> orderService.updateStatus(1L, "7", OrderStatus.SHIPPED));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void ownerCanAdvanceTheStatus() {
        Order own = new Order();
        own.setUserId("7");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(own));
        repositoryEchoesSaves();

        assertEquals(OrderStatus.SHIPPED, orderService.updateStatus(1L, "7", OrderStatus.SHIPPED).getStatus());
    }
}
