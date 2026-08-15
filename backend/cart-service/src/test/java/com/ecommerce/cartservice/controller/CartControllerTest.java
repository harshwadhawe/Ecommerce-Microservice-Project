package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.exception.ProductNotAvailableException;
import com.ecommerce.cartservice.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    private Cart cartWithOneItem() {
        Cart cart = new Cart("user-1");
        cart.addItem(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 2, null));
        return cart;
    }

    @Test
    void returnsCartWithComputedTotals() throws Exception {
        when(cartService.getCartByUserId("user-1")).thenReturn(cartWithOneItem());

        mockMvc.perform(get("/api/cart/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalAmount").value(1999.98));
    }

    @Test
    void addItemReturnsUpdatedCart() throws Exception {
        when(cartService.addToCart(anyString(), any())).thenReturn(cartWithOneItem());

        mockMvc.perform(post("/api/cart/user-1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p1\",\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value("p1"));
    }

    @Test
    void unavailableProductMapsToBadRequest() throws Exception {
        when(cartService.addToCart(anyString(), any()))
                .thenThrow(new ProductNotAvailableException("Insufficient stock available"));

        mockMvc.perform(post("/api/cart/user-1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p1\",\"quantity\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Insufficient stock available"));
    }

    @Test
    void rejectsQuantityBelowOneOnAdd() throws Exception {
        mockMvc.perform(post("/api/cart/user-1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p1\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").exists());
    }

    @Test
    void rejectsBlankProductIdOnAdd() throws Exception {
        mockMvc.perform(post("/api/cart/user-1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"\",\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.productId").exists());
    }

    @Test
    void clearCartReturnsConfirmationMessage() throws Exception {
        mockMvc.perform(delete("/api/cart/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared successfully"));
    }

    @Test
    void validateEndpointExposesStockValidity() throws Exception {
        when(cartService.validateCartStock("user-1")).thenReturn(false);

        mockMvc.perform(get("/api/cart/user-1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }
}
