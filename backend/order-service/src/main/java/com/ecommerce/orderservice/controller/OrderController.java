package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.CreateOrderDto;
import com.ecommerce.orderservice.dto.UpdateOrderStatusDto;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Identity always comes from the verified token, never from the request body or a path variable, so
 * there is nothing for a caller to forge. The raw Authorization header is forwarded to cart-service,
 * which does its own check.
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @Valid @RequestBody CreateOrderDto request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            Authentication authentication) {
        Order order = orderService.placeOrder(authentication.getName(), bearerToken, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public ResponseEntity<List<Order>> myOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrdersForUser(authentication.getName()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId, Authentication authentication) {
        return ResponseEntity.ok(orderService.getOrderForUser(orderId, authentication.getName()));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusDto request,
            Authentication authentication) {
        return ResponseEntity.ok(
                orderService.updateStatus(orderId, authentication.getName(), request.getStatus()));
    }
}
