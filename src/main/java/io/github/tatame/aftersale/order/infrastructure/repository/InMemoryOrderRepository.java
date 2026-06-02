package io.github.tatame.aftersale.order.infrastructure.repository;

import io.github.tatame.aftersale.order.domain.Order;
import io.github.tatame.aftersale.order.domain.OrderItem;
import io.github.tatame.aftersale.order.domain.OrderRepository;
import io.github.tatame.aftersale.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

/**
 * 提供默认 profile 下的内存订单仓储和演示种子数据。
 *
 * <p>边界：该实现服务于离线测试和轻量本地运行，默认 mvn test 不应依赖 MySQL、Docker 或外部网络。
 */
@Repository
@Profile("!mysql")
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
        // 种子订单覆盖售后窗口、物流异常、特殊商品等演示分支，避免测试依赖真实订单系统。
        return List.of(
                order(
                        "O-PAID-NOT-SHIPPED",
                        "U-DEMO-ORDER",
                        "P-KEYBOARD-001",
                        "Mechanical Keyboard",
                        OrderStatus.PAID,
                        "299.00",
                        "2026-05-12T10:00:00Z",
                        "2026-06-12T23:59:59Z",
                        "计算机"),
                order(
                        "O202605130001",
                        "U-DEMO-1",
                        "P-HEADPHONE-001",
                        "Wireless Headphones",
                        OrderStatus.DELIVERED,
                        "499.00",
                        "2026-05-01T09:00:00Z",
                        "2026-05-10T15:00:00Z",
                        "2026-05-25T23:59:59Z",
                        "电子数码"),
                order(
                        "O-7001",
                        "U-7001",
                        "P-HEADPHONE-001",
                        "Wireless Headphones",
                        OrderStatus.DELIVERED,
                        "499.00",
                        "2026-05-01T09:00:00Z",
                        "2026-05-10T15:00:00Z",
                        "2026-05-25T23:59:59Z",
                        "电子数码"),
                order(
                        "O-7002",
                        "U-7002",
                        "P-CHARGER-001",
                        "USB-C Charger",
                        OrderStatus.LOGISTICS_EXCEPTION,
                        "89.00",
                        "2026-05-03T12:00:00Z",
                        "2026-06-03T23:59:59Z",
                        "电子数码"),
                order(
                        "O-EXPIRED-AFTERSALE",
                        "U-DEMO-ORDER",
                        "P-SHOES-001",
                        "Running Shoes",
                        OrderStatus.DELIVERED,
                        "399.00",
                        "2026-03-01T09:00:00Z",
                        "2026-03-05T14:00:00Z",
                        "2026-03-20T23:59:59Z",
                        "服饰鞋包"),
                order(
                        "O-SPECIAL-GOODS",
                        "U-DEMO-ORDER",
                        "P-CUSTOM-001",
                        "Customized Gift Box",
                        OrderStatus.DELIVERED,
                        "199.00",
                        "2026-05-08T08:30:00Z",
                        "2026-05-12T18:00:00Z",
                        "2026-05-19T23:59:59Z",
                        "特殊商品"));
    }

    private static Order order(
            String orderId,
            String userId,
            String productId,
            String productName,
            OrderStatus orderStatus,
            String paidAmount,
            String paidAt,
            String aftersaleDeadline,
            String category) {
        return order(
                orderId,
                userId,
                productId,
                productName,
                orderStatus,
                paidAmount,
                paidAt,
                null,
                aftersaleDeadline,
                category);
    }

    private static Order order(
            String orderId,
            String userId,
            String productId,
            String productName,
            OrderStatus orderStatus,
            String paidAmount,
            String paidAt,
            @Nullable String deliveredAt,
            String aftersaleDeadline,
            String category) {
        BigDecimal amount = new BigDecimal(paidAmount);
        return new Order(
                orderId,
                userId,
                productId,
                productName,
                orderStatus,
                amount,
                Instant.parse(paidAt),
                deliveredAt == null ? null : Instant.parse(deliveredAt),
                Instant.parse(aftersaleDeadline),
                List.of(OrderItem.fromOrderLine(
                        "OI-" + orderId + "-1",
                        productId,
                        productName,
                        category,
                        1,
                        amount,
                        orderStatus)));
    }
}
