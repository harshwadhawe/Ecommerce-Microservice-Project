package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.AddToCartDto;
import com.ecommerce.cartservice.dto.UpdateCartItemDto;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
// A cart belongs to the user whose id is in the token. Applied at class level so a new endpoint
// cannot forget it; every handler here takes {userId}, which is what authentication.name holds.
@PreAuthorize("#userId == authentication.name")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable String userId) {
        Cart cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<Cart> addToCart(@PathVariable String userId, @Valid @RequestBody AddToCartDto addToCartDto) {
        Cart cart = cartService.addToCart(userId, addToCartDto);
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/{userId}/items/{productId}")
    public ResponseEntity<Cart> updateCartItem(@PathVariable String userId, @PathVariable String productId, @Valid @RequestBody UpdateCartItemDto updateDto) {
        Cart cart = cartService.updateCartItemQuantity(userId, productId, updateDto.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<Cart> removeFromCart(@PathVariable String userId, @PathVariable String productId) {
        Cart cart = cartService.removeFromCart(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> clearCart(@PathVariable String userId) {
        cartService.clearCart(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cart cleared successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/validate")
    public ResponseEntity<Map<String, Boolean>> validateCart(@PathVariable String userId) {
        boolean isValid = cartService.validateCartStock(userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("valid", isValid);
        return ResponseEntity.ok(response);
    }
}