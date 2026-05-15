package com.example.aftersale.order.infrastructure.repository;

import com.example.aftersale.order.domain.Order;
import com.example.aftersale.order.domain.OrderRepository;
import com.example.aftersale.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public InMemoryOrderRepository() {
        seedOrders().forEach(order -> orders.put(order.getOrderId(), order));
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return orders.values().stream()
                .filter(order -> order.getUserId().equals(userId))
                .sorted(Comparator.comparing(Order::getPaidAt).reversed())
                .toList();
    }

    private static List<Order> seedOrders() {
        return List.of(
                order(
                        "O-PAID-NOT-SHIPPED",
                        "U-DEMO-ORDER",
                        "P-KEYBOARD-001",
                        "Mechanical Keyboard",
                        OrderStatus.PAID,
                        "299.00",
                        "2026-05-12T10:00:00Z",
                        "2026-06-12T23:59:59Z"),
                order(
                        "O202605130001",
                        "U-DEMO-1",
                        "P-HEADPHONE-001",
                        "Wireless Headphones",
                        OrderStatus.DELIVERED,
                        "499.00",
                        "2026-05-01T09:00:00Z",
                        "2026-05-10T15:00:00Z",
                        "2026-05-25T23:59:59Z"),
                order(
                        "O-7001",
                        "U-7001",
                        "P-HEADPHONE-001",
                        "Wireless Headphones",
                        OrderStatus.DELIVERED,
                        "499.00",
                        "2026-05-01T09:00:00Z",
                        "2026-05-10T15:00:00Z",
                        "2026-05-25T23:59:59Z"),
                order(
                        "O-7002",
                        "U-7002",
                        "P-CHARGER-001",
                        "USB-C Charger",
                        OrderStatus.LOGISTICS_EXCEPTION,
                        "89.00",
                        "2026-05-03T12:00:00Z",
                        "2026-06-03T23:59:59Z"),
                order(
                        "O-EXPIRED-AFTERSALE",
                        "U-DEMO-ORDER",
                        "P-SHOES-001",
                        "Running Shoes",
                        OrderStatus.DELIVERED,
                        "399.00",
                        "2026-03-01T09:00:00Z",
                        "2026-03-05T14:00:00Z",
                        "2026-03-20T23:59:59Z"),
                order(
                        "O-SPECIAL-GOODS",
                        "U-DEMO-ORDER",
                        "P-CUSTOM-001",
                        "Customized Gift Box",
                        OrderStatus.DELIVERED,
                        "199.00",
                        "2026-05-08T08:30:00Z",
                        "2026-05-12T18:00:00Z",
                        "2026-05-19T23:59:59Z"));
    }

    private static Order order(
            String orderId,
            String userId,
            String productId,
            String productName,
            OrderStatus orderStatus,
            String paidAmount,
            String paidAt,
            String aftersaleDeadline) {
        return new Order(
                orderId,
                userId,
                productId,
                productName,
                orderStatus,
                new BigDecimal(paidAmount),
                Instant.parse(paidAt),
                null,
                Instant.parse(aftersaleDeadline));
    }

    private static Order order(
            String orderId,
            String userId,
            String productId,
            String productName,
            OrderStatus orderStatus,
            String paidAmount,
            String paidAt,
            String deliveredAt,
            String aftersaleDeadline) {
        return new Order(
                orderId,
                userId,
                productId,
                productName,
                orderStatus,
                new BigDecimal(paidAmount),
                Instant.parse(paidAt),
                deliveredAt == null ? null : Instant.parse(deliveredAt),
                Instant.parse(aftersaleDeadline));
    }
}
