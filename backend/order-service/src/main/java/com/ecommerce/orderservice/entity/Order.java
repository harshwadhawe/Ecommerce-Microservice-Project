package com.ecommerce.orderservice.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-facing reference; the numeric id stays internal.
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "payment_transaction_id")
    private String paymentTransactionId;

    @Column(name = "payment_message")
    private String paymentMessage;

    @Column(name = "recipient_name")
    private String recipientName;

    private String address;
    private String city;
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    /**
     * Items are copied, not referenced: an order must keep the price and name as they were when it
     * was placed, even after the catalog changes.
     */
    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
        recalculateTotal();
    }

    public void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.updatedAt = LocalDateTime.now();
    }

    public void markPaid(String transactionId, String message) {
        this.status = OrderStatus.PAID;
        this.paymentTransactionId = transactionId;
        this.paymentMessage = message;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPaymentFailed(String transactionId, String reason) {
        this.status = OrderStatus.PAYMENT_FAILED;
        this.paymentTransactionId = transactionId;
        this.paymentMessage = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(OrderStatus next) {
        this.status = next;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getTotalItems() {
        return items.stream().mapToInt(OrderItem::getQuantity).sum();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    public String getPaymentMessage() {
        return paymentMessage;
    }

    public void setPaymentMessage(String paymentMessage) {
        this.paymentMessage = paymentMessage;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
