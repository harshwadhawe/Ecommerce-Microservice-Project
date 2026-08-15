package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.config.SecurityConfig;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.security.JwtAuthenticationFilter;
import com.ecommerce.cartservice.service.CartService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A cart used to be readable and clearable by anyone who could guess a user id. These tests run the
 * real filter chain, so they fail if the JWT check or the ownership rule is removed.
 */
@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "jwt.secret=" + CartAuthorizationTest.SECRET)
class CartAuthorizationTest {

    static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256!!";

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    private String tokenFor(Object userId, long expiresInMs) {
        return Jwts.builder()
                .setClaims(Map.of("userId", userId))
                .setSubject("a@b.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void ownerCanReadTheirCart() throws Exception {
        when(cartService.getCartByUserId("7")).thenReturn(new Cart("7"));

        mockMvc.perform(get("/api/cart/7").header("Authorization", "Bearer " + tokenFor(7, 60000)))
                .andExpect(status().isOk());
    }

    @Test
    void anotherUsersCartIsForbidden() throws Exception {
        mockMvc.perform(get("/api/cart/8").header("Authorization", "Bearer " + tokenFor(7, 60000)))
                .andExpect(status().isForbidden());

        verify(cartService, never()).getCartByUserId(anyString());
    }

    @Test
    void anotherUsersCartCannotBeCleared() throws Exception {
        mockMvc.perform(delete("/api/cart/8").header("Authorization", "Bearer " + tokenFor(7, 60000)))
                .andExpect(status().isForbidden());

        verify(cartService, never()).clearCart(anyString());
    }

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cart/7")).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cart/7").header("Authorization", "Bearer " + tokenFor(7, -60000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherSecretIsUnauthorized() throws Exception {
        String forged = Jwts.builder()
                .setClaims(Map.of("userId", 7))
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor("a-completely-different-secret-key-of-len!!".getBytes()),
                        SignatureAlgorithm.HS256)
                .compact();

        mockMvc.perform(get("/api/cart/7").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cart/7").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
