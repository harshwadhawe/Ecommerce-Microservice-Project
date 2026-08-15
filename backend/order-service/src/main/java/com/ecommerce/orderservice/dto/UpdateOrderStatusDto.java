package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateOrderStatusDto {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    public UpdateOrderStatusDto() {}

    public UpdateOrderStatusDto(OrderStatus status) { this.status = status; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
