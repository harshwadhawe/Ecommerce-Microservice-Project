package com.ecommerce.cartservice.config;

import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Carts are stored in Redis as JSON produced by this exact mapper. If it cannot read its own
 * output back, every cart read fails, CartRepository swallows the exception and hands the caller
 * a fresh empty cart -- carts silently reset on every request.
 */
class CartSerializationTest {

    private final ObjectMapper mapper = new RedisConfig().objectMapper();

    @Test
    void cartSurvivesARoundTripThroughTheRedisMapper() throws Exception {
        Cart cart = new Cart("user-1");
        cart.addItem(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 2, "img.png"));
        cart.addItem(new CartItem("p2", "Mouse", new BigDecimal("10.50"), 1, null));

        Cart restored = mapper.readValue(mapper.writeValueAsString(cart), Cart.class);

        assertEquals("user-1", restored.getUserId());
        assertEquals(2, restored.getItems().size());
        assertEquals(3, restored.getTotalItems());
        assertEquals(0, new BigDecimal("2010.48").compareTo(restored.getTotalAmount()));
        assertEquals("Laptop", restored.findItemByProductId("p1").getProductName());
        assertEquals(cart.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    void emptyCartSurvivesARoundTrip() throws Exception {
        Cart restored = mapper.readValue(mapper.writeValueAsString(new Cart("user-2")), Cart.class);

        assertEquals("user-2", restored.getUserId());
        assertEquals(0, restored.getTotalItems());
    }
}
