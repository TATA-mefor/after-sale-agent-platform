package com.example.aftersale.order.domain;

public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    LOGISTICS_EXCEPTION,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    REFUNDED
}
