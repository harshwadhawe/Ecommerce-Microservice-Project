package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.entity.Order;

/**
 * Carries the saved order so the caller learns the order number of the failed attempt, not just
 * that something went wrong.
 */
public class PaymentDeclinedException extends RuntimeException {

    private final transient Order order;

    public PaymentDeclinedException(String message, Order order) {
        super(message);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
