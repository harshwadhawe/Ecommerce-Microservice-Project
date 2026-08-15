package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Shape of cart-service's response; only the fields an order needs are mapped. */
public class CartDto {

    private String userId;
    private List<CartItemDto> items = new ArrayList<>();
    private BigDecimal totalAmount;
    private Integer totalItems;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
}
