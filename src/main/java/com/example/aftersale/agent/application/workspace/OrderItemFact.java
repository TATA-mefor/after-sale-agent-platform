package com.example.aftersale.agent.application.workspace;

import java.util.Map;
import java.util.Objects;

/**
 * 保存订单明细级事实，供退货、换货等子任务判断目标商品。
 *
 * <p>边界：supportReturn 和 supportExchange 是工具层输出的派生事实，不是订单数据库字段，也不能绕过政策校验。
 */
public record OrderItemFact(
        String orderItemId,
        String productId,
        String productName,
        String category,
        String quantity,
        String unitPrice,
        String itemStatus,
        boolean supportReturn,
        boolean supportExchange,
        boolean specialItem) {

    public OrderItemFact {
        orderItemId = requireText(orderItemId, "orderItemId");
        productId = requireText(productId, "productId");
        productName = requireText(productName, "productName");
        category = requireText(category, "category");
        quantity = requireText(quantity, "quantity");
        unitPrice = requireText(unitPrice, "unitPrice");
        itemStatus = requireText(itemStatus, "itemStatus");
    }

    public static OrderItemFact fromToolItem(Map<?, ?> data) {
        return new OrderItemFact(
                text(data, "orderItemId"),
                text(data, "productId"),
                text(data, "productName"),
                text(data, "category"),
                text(data, "quantity"),
                text(data, "unitPrice"),
                text(data, "itemStatus"),
                booleanValue(data, "supportReturn"),
                booleanValue(data, "supportExchange"),
                booleanValue(data, "isSpecialItem"));
    }

    public String summary() {
        return orderItemId
                + " productId=" + productId
                + " productName=" + productName
                + " category=" + category
                + " quantity=" + quantity
                + " unitPrice=" + unitPrice
                + " itemStatus=" + itemStatus
                + " supportReturn=" + supportReturn
                + " supportExchange=" + supportExchange
                + " isSpecialItem=" + specialItem;
    }

    private static String text(Map<?, ?> data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Object value = data.get(key);
        if (value == null) {
            return "N/A";
        }
        return value.toString();
    }

    private static boolean booleanValue(Map<?, ?> data, String key) {
        Objects.requireNonNull(data, "data must not be null");
        Object value = data.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(text(data, key));
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
