package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;

public class PaymentResponseDto {

    private String transactionId;
    private String orderId;
    private BigDecimal amount;
    private String status;
    private String message;

    public boolean isSuccessful() { return "SUCCESS".equals(status); }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
