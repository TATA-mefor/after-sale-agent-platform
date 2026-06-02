package io.github.tatame.aftersale.order.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

public final class OrderItem {

    private final String orderItemId;
    private final String productId;
    private final String productName;
    private final String category;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final String itemStatus;
    private final boolean supportReturn;
    private final boolean supportExchange;
    private final boolean specialItem;

    public OrderItem(
            String orderItemId,
            String productId,
            String productName,
            String category,
            int quantity,
            BigDecimal unitPrice,
            String itemStatus,
            boolean supportReturn,
            boolean supportExchange,
            boolean specialItem) {
        this.orderItemId = requireText(orderItemId, "orderItemId");
        this.productId = requireText(productId, "productId");
        this.productName = requireText(productName, "productName");
        this.category = requireText(category, "category");
        this.quantity = requirePositiveQuantity(quantity);
        this.unitPrice = requireNonNegativeAmount(unitPrice);
        this.itemStatus = requireText(itemStatus, "itemStatus");
        this.supportReturn = supportReturn;
        this.supportExchange = supportExchange;
        this.specialItem = specialItem;
    }

    public static OrderItem fromOrderLine(
            String orderItemId,
            String productId,
            String productName,
            String category,
            int quantity,
            BigDecimal unitPrice,
            OrderStatus orderStatus) {
        boolean special = isSpecialCategory(category) || isSpecialProduct(productId) || isSpecialProduct(productName);
        return new OrderItem(
                orderItemId,
                productId,
                productName,
                category,
                quantity,
                unitPrice,
                orderStatus.name(),
                !special,
                !special,
                special);
    }

    public String getOrderItemId() {
        return orderItemId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategory() {
        return category;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getItemStatus() {
        return itemStatus;
    }

    public boolean isSupportReturn() {
        return supportReturn;
    }

    public boolean isSupportExchange() {
        return supportExchange;
    }

    public boolean isSpecialItem() {
        return specialItem;
    }

    private static boolean isSpecialCategory(String value) {
        String normalized = normalize(value);
        return normalized.contains("特殊")
                || normalized.contains("定制")
                || normalized.contains("custom");
    }

    private static boolean isSpecialProduct(String value) {
        String normalized = normalize(value);
        return normalized.contains("定制")
                || normalized.contains("custom")
                || normalized.contains("customized");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static int requirePositiveQuantity(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return value;
    }

    private static BigDecimal requireNonNegativeAmount(BigDecimal value) {
        Objects.requireNonNull(value, "unitPrice must not be null");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must not be negative");
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
