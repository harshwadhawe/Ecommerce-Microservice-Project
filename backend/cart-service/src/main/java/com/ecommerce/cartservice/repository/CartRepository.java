package com.ecommerce.cartservice.repository;

import com.ecommerce.cartservice.entity.Cart;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class CartRepository {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_EXPIRATION_HOURS = 24;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public Optional<Cart> findByUserId(String userId) {
        try {
            String key = CART_KEY_PREFIX + userId;
            Object cartData = redisTemplate.opsForValue().get(key);
            if (cartData != null) {
                String json = cartData.toString();
                Cart cart = objectMapper.readValue(json, Cart.class);
                return Optional.of(cart);
            }
        } catch (Exception e) {
            // Log error appropriately
        }
        return Optional.empty();
    }

    public Cart save(Cart cart) {
        try {
            String key = CART_KEY_PREFIX + cart.getUserId();
            String json = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(key, json, CART_EXPIRATION_HOURS, TimeUnit.HOURS);
            return cart;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save cart", e);
        }
    }

    public void deleteByUserId(String userId) {
        String key = CART_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    public boolean existsByUserId(String userId) {
        String key = CART_KEY_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void extendExpiration(String userId) {
        String key = CART_KEY_PREFIX + userId;
        redisTemplate.expire(key, CART_EXPIRATION_HOURS, TimeUnit.HOURS);
    }
}