package com.ecommerce.orderservice.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class OrderTest {

    private OrderItem item(String id, String price, int quantity) {
        return new OrderItem(id, "product-" + id, new BigDecimal(price), quantity, null);
    }

    @Test
    void totalIsTheSumOfLineSubtotals() {
        Order order = new Order();
        order.addItem(item("p1", "999.99", 1));
        order.addItem(item("p2", "49.99", 2));

        assertEquals(0, new BigDecimal("1099.97").compareTo(order.getTotalAmount()));
        assertEquals(3, order.getTotalItems());
    }

    @Test
    void addingAnItemLinksItBackToTheOrder() {
        Order order = new Order();
        OrderItem line = item("p1", "10.00", 1);

        order.addItem(line);

        assertSame(order, line.getOrder());
    }

    @Test
    void subtotalUsesExactDecimalArithmetic() {
        assertEquals(0, new BigDecimal("0.30").compareTo(item("p1", "0.10", 3).getSubtotal()));
    }

    @Test
    void aNewOrderStartsPendingAndEmpty() {
        Order order = new Order();

        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(order.getTotalAmount()));
        assertEquals(0, order.getTotalItems());
    }

    @Test
    void markingPaidRecordsTheTransaction() {
        Order order = new Order();

        order.markPaid("txn-9", "Payment processed successfully");

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals("txn-9", order.getPaymentTransactionId());
    }

    @Test
    void markingFailedKeepsTheReason() {
        Order order = new Order();

        order.markPaymentFailed("txn-9", "Card declined");

        assertEquals(OrderStatus.PAYMENT_FAILED, order.getStatus());
        assertEquals("Card declined", order.getPaymentMessage());
    }
}
