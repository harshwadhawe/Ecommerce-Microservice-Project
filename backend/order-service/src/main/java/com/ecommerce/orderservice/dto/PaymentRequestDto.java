package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;

public class PaymentRequestDto {

    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String cardNumber;
    private String cvv;
    private String expiryDate;
    private String cardholderName;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
}
