package com.example.aftersale.tool.application.order;

import com.example.aftersale.order.domain.Order;
import com.example.aftersale.order.domain.OrderItem;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class OrderToolOutput {

    private OrderToolOutput() {
    }

    static Map<String, Object> toMap(Order order, Instant checkedAt) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("userId", order.getUserId());
        data.put("productId", order.getProductId());
        data.put("productName", order.getProductName());
        data.put("orderStatus", order.getOrderStatus().name());
        data.put("paidAmount", order.getPaidAmount());
        data.put("paidAt", order.getPaidAt().toString());
        data.put("deliveredAt", order.getDeliveredAt() == null ? "N/A" : order.getDeliveredAt().toString());
        data.put("aftersaleDeadline", order.getAftersaleDeadline().toString());
        data.put("whetherInAftersaleWindow", order.isWithinAfterSaleWindow(checkedAt));
        data.put("orderItems", order.getOrderItems().stream()
                .map(OrderToolOutput::orderItemToMap)
                .toList());
        return data;
    }

    private static Map<String, Object> orderItemToMap(OrderItem orderItem) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderItemId", orderItem.getOrderItemId());
        data.put("productId", orderItem.getProductId());
        data.put("productName", orderItem.getProductName());
        data.put("category", orderItem.getCategory());
        data.put("quantity", orderItem.getQuantity());
        data.put("unitPrice", orderItem.getUnitPrice());
        data.put("itemStatus", orderItem.getItemStatus());
        data.put("supportReturn", orderItem.isSupportReturn());
        data.put("supportExchange", orderItem.isSupportExchange());
        data.put("isSpecialItem", orderItem.isSpecialItem());
        return data;
    }

    static List<Map<String, Object>> orderItemsToMap(Order order) {
        return order.getOrderItems().stream()
                .map(OrderToolOutput::orderItemToMap)
                .toList();
    }
}
