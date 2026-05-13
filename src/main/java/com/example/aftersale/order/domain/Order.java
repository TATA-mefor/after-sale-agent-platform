package com.example.aftersale.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Order {

    private final String orderId;
    private final String userId;
    private final String productId;
    private final String productName;
    private final OrderStatus orderStatus;
    private final BigDecimal paidAmount;
    private final Instant paidAt;
    private final Instant deliveredAt;
    private final Instant aftersaleDeadline;

    public Order(
            String orderId,
            String userId,
            String productId,
            String productName,
            OrderStatus orderStatus,
            BigDecimal paidAmount,
            Instant paidAt,
            Instant deliveredAt,
            Instant aftersaleDeadline) {
        this.orderId = requireText(orderId, "orderId");
        this.userId = requireText(userId, "userId");
        this.productId = requireText(productId, "productId");
        this.productName = requireText(productName, "productName");
        this.orderStatus = Objects.requireNonNull(orderStatus, "orderStatus must not be null");
        this.paidAmount = requireNonNegativeAmount(paidAmount);
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt must not be null");
        this.deliveredAt = deliveredAt;
        this.aftersaleDeadline = Objects.requireNonNull(aftersaleDeadline, "aftersaleDeadline must not be null");
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getAftersaleDeadline() {
        return aftersaleDeadline;
    }

    public boolean isWithinAfterSaleWindow(Instant checkedAt) {
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");
        return !checkedAt.isAfter(aftersaleDeadline);
    }

    private static BigDecimal requireNonNegativeAmount(BigDecimal value) {
        Objects.requireNonNull(value, "paidAmount must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("paidAmount must not be negative");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
