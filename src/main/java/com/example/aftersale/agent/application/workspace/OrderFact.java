package com.example.aftersale.agent.application.workspace;

import java.util.Map;
import java.util.Objects;

public record OrderFact(
        String orderId,
        String productName,
        String orderStatus,
        String paidAmount,
        String deliveredAt,
        String aftersaleDeadline,
        boolean whetherInAftersaleWindow,
        String sourceToolName,
        String subtaskId) {

    public OrderFact {
        orderId = requireText(orderId, "orderId");
        productName = requireText(productName, "productName");
        orderStatus = requireText(orderStatus, "orderStatus");
        paidAmount = requireText(paidAmount, "paidAmount");
        deliveredAt = requireText(deliveredAt, "deliveredAt");
        aftersaleDeadline = requireText(aftersaleDeadline, "aftersaleDeadline");
        sourceToolName = requireText(sourceToolName, "sourceToolName");
        subtaskId = subtaskId == null ? "" : subtaskId;
    }

    public static OrderFact fromToolData(
            String sourceToolName,
            String subtaskId,
            Map<String, Object> data) {
        return new OrderFact(
                text(data, "orderId"),
                text(data, "productName"),
                text(data, "orderStatus"),
                text(data, "paidAmount"),
                text(data, "deliveredAt"),
                text(data, "aftersaleDeadline"),
                Boolean.TRUE.equals(data.get("whetherInAftersaleWindow")),
                sourceToolName,
                subtaskId);
    }

    public String summary() {
        return "Order " + orderId
                + ": " + productName
                + ", status=" + orderStatus
                + ", aftersaleWindow=" + whetherInAftersaleWindow
                + ", deadline=" + aftersaleDeadline;
    }

    private static String text(Map<String, Object> data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Object value = data.get(key);
        if (value == null) {
            return "N/A";
        }
        return value.toString();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
