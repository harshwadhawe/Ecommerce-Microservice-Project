package com.ecommerce.cartservice.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTest {

    private CartItem item(String productId, String price, int quantity) {
        return new CartItem(productId, "name-" + productId, new BigDecimal(price), quantity, null);
    }

    @Test
    void addingSameProductTwiceMergesQuantities() {
        Cart cart = new Cart("user-1");
        cart.addItem(item("p1", "10.00", 1));
        cart.addItem(item("p1", "10.00", 2));

        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getTotalItems());
        assertEquals(0, new BigDecimal("30.00").compareTo(cart.getTotalAmount()));
    }

    @Test
    void distinctProductsStaySeparate() {
        Cart cart = new Cart("user-1");
        cart.addItem(item("p1", "10.00", 1));
        cart.addItem(item("p2", "5.50", 2));

        assertEquals(2, cart.getItems().size());
        assertEquals(3, cart.getTotalItems());
        assertEquals(0, new BigDecimal("21.00").compareTo(cart.getTotalAmount()));
    }

    @Test
    void updatingQuantityToZeroRemovesTheItem() {
        Cart cart = new Cart("user-1");
        cart.addItem(item("p1", "10.00", 3));

        cart.updateItemQuantity("p1", 0);

        assertTrue(cart.getItems().isEmpty());
        assertNull(cart.findItemByProductId("p1"));
    }

    @Test
    void negativeQuantityAlsoRemovesTheItem() {
        Cart cart = new Cart("user-1");
        cart.addItem(item("p1", "10.00", 3));

        cart.updateItemQuantity("p1", -2);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void updatingUnknownProductIsANoop() {
        Cart cart = new Cart("user-1");
        cart.addItem(item("p1", "10.00", 1));

        cart.updateItemQuantity("missing", 9);

        assertEquals(1, cart.getTotalItems());
    }

    @Test
    void emptyCartTotalsAreZero() {
        Cart cart = new Cart("user-1");

        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getTotalAmount()));
        assertEquals(0, cart.getTotalItems());
    }

    @Test
    void clearCartDropsEveryItemButKeepsTheOwner() {
        Cart cart = new Cart("user-1");
        cart.addItem(item("p1", "10.00", 1));
        cart.addItem(item("p2", "1.00", 1));

        cart.clearCart();

        assertTrue(cart.getItems().isEmpty());
        assertEquals("user-1", cart.getUserId());
    }

    @Test
    void subtotalUsesExactDecimalArithmetic() {
        assertEquals(0, new BigDecimal("0.30").compareTo(item("p1", "0.10", 3).getSubtotal()));
    }

    @Test
    void mutationsBumpTheUpdatedTimestamp() throws InterruptedException {
        Cart cart = new Cart("user-1");
        java.time.LocalDateTime before = cart.getUpdatedAt();
        Thread.sleep(2);

        cart.addItem(item("p1", "10.00", 1));

        assertTrue(cart.getUpdatedAt().isAfter(before));
    }
}
