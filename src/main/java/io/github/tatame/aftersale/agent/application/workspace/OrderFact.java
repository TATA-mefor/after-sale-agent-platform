package io.github.tatame.aftersale.agent.application.workspace;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 保存订单查询工具提炼出的订单事实。
 *
 * <p>边界：该结构只属于单次 AgentRun 的工作记忆，不能替代订单领域模型，也不能作为审计记录来源。
 */
public record OrderFact(
        String orderId,
        String productName,
        String orderStatus,
        String paidAmount,
        String deliveredAt,
        String aftersaleDeadline,
        boolean whetherInAftersaleWindow,
        String itemSummary,
        List<OrderItemFact> orderItems,
        String sourceToolName,
        String subtaskId) {

    public OrderFact {
        orderId = requireText(orderId, "orderId");
        productName = requireText(productName, "productName");
        orderStatus = requireText(orderStatus, "orderStatus");
        paidAmount = requireText(paidAmount, "paidAmount");
        deliveredAt = requireText(deliveredAt, "deliveredAt");
        aftersaleDeadline = requireText(aftersaleDeadline, "aftersaleDeadline");
        itemSummary = requireText(itemSummary, "itemSummary");
        orderItems = List.copyOf(Objects.requireNonNull(orderItems, "orderItems must not be null"));
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
                itemSummary(data.get("orderItems")),
                orderItems(data.get("orderItems")),
                sourceToolName,
                subtaskId);
    }

    public String summary() {
        return "Order " + orderId
                + ": " + productName
                + ", status=" + orderStatus
                + ", aftersaleWindow=" + whetherInAftersaleWindow
                + ", items=" + itemSummary
                + ", deadline=" + aftersaleDeadline;
    }

    private static String itemSummary(Object value) {
        if (!(value instanceof List<?> items) || items.isEmpty()) {
            return "N/A";
        }
        return items.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<?, ?>) item)
                .map(item -> text(item, "productName") + " x" + text(item, "quantity")
                        + " category=" + text(item, "category"))
                .collect(Collectors.joining(", "));
    }

    private static List<OrderItemFact> orderItems(Object value) {
        if (!(value instanceof List<?> items) || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<?, ?>) item)
                .map(OrderItemFact::fromToolItem)
                .toList();
    }

    private static String text(Map<?, ?> data, String key) {
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
