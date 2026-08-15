package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.EmptyCartException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.PaymentDeclinedException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository orderRepository, CartClient cartClient, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.cartClient = cartClient;
        this.paymentClient = paymentClient;
    }

    /**
     * Checkout, in the order that keeps the books straight:
     * <ol>
     *   <li>snapshot the cart into a PENDING order and persist it, so a crash mid-payment still
     *       leaves a record of what was attempted;</li>
     *   <li>charge payment-service;</li>
     *   <li>on success mark PAID and empty the cart, on decline mark PAYMENT_FAILED and leave the
     *       cart alone so the shopper can retry.</li>
     * </ol>
     */
    // noRollbackFor is load-bearing: a declined payment throws, and without this the rollback would
    // erase the PAYMENT_FAILED row this method just wrote, losing the record of the attempt.
    @Transactional(noRollbackFor = PaymentDeclinedException.class)
    public Order placeOrder(String userId, String bearerToken, CreateOrderDto request) {
        CartDto cart = cartClient.getCart(userId, bearerToken);

        if (cart == null || cart.getItems().isEmpty()) {
            throw new EmptyCartException("Your cart is empty");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNumber(nextOrderNumber());
        order.setRecipientName(request.getRecipientName());
        order.setAddress(request.getAddress());
        order.setCity(request.getCity());
        order.setCountry(request.getCountry());
        order.setPostalCode(request.getPostalCode());

        for (CartItemDto item : cart.getItems()) {
            order.addItem(new OrderItem(
                    item.getProductId(),
                    item.getProductName(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getImageUrl()));
        }

        Order pending = orderRepository.save(order);

        PaymentRequestDto payment = new PaymentRequestDto();
        payment.setOrderId(pending.getOrderNumber());
        payment.setAmount(pending.getTotalAmount());
        payment.setPaymentMethod("CARD");
        payment.setCardNumber(request.getCardNumber());
        payment.setCvv(request.getCvv());
        payment.setExpiryDate(request.getExpiryDate());
        payment.setCardholderName(request.getCardholderName());

        PaymentResponseDto result = paymentClient.charge(payment);

        if (result == null || !result.isSuccessful()) {
            String reason = result == null ? "Payment could not be completed" : result.getMessage();
            pending.markPaymentFailed(result == null ? null : result.getTransactionId(), reason);
            orderRepository.save(pending);
            log.info("Order {} declined: {}", pending.getOrderNumber(), reason);
            throw new PaymentDeclinedException(reason, pending);
        }

        pending.markPaid(result.getTransactionId(), result.getMessage());
        Order paid = orderRepository.save(pending);

        // The cart is emptied only after the money is taken. If this throws, the order still
        // stands -- a duplicated cart is recoverable, a paid-for order that vanished is not.
        try {
            cartClient.clearCart(userId, bearerToken);
        } catch (RuntimeException e) {
            log.warn("Order {} is paid but its cart could not be cleared", paid.getOrderNumber(), e);
        }

        return paid;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersForUser(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Order getOrderForUser(Long orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        // Checked here rather than in the query so a wrong owner is a 403, not a misleading 404.
        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("This order belongs to another user");
        }
        return order;
    }

    @Transactional
    public Order updateStatus(Long orderId, String userId, OrderStatus status) {
        Order order = getOrderForUser(orderId, userId);
        order.changeStatus(status);
        return orderRepository.save(order);
    }

    private String nextOrderNumber() {
        return "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
