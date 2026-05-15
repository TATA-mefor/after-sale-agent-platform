package com.example.aftersale.tool.application.order;

import com.example.aftersale.order.domain.Order;
import java.time.Instant;
import java.util.LinkedHashMap;
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
        return data;
    }
}
