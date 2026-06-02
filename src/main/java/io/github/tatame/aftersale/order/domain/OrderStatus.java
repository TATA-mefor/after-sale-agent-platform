package io.github.tatame.aftersale.order.domain;

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
