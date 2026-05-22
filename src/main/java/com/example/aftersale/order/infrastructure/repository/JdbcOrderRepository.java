package com.example.aftersale.order.infrastructure.repository;

import com.example.aftersale.order.domain.Order;
import com.example.aftersale.order.domain.OrderItem;
import com.example.aftersale.order.domain.OrderRepository;
import com.example.aftersale.order.domain.OrderStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 在 mysql profile 下通过 JDBC 读取订单数据。
 *
 * <p>边界：该实现位于 infrastructure 层，负责把表结构映射为领域对象；domain 层不能依赖 JDBC 或
 * Spring 数据库框架，默认测试也不能强依赖该 profile。
 */
@Repository
@Profile("mysql")
public class JdbcOrderRepository implements OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JdbcTemplate is a Spring-managed infrastructure collaborator.")
    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return jdbcTemplate.query("""
                SELECT order_id, user_id, product_id, product_name, order_status,
                       paid_amount, paid_at, delivered_at, aftersale_deadline
                FROM orders
                WHERE order_id = ?
                """, (resultSet, rowNumber) -> mapOrder(resultSet), orderId).stream().findFirst();
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return jdbcTemplate.query("""
                SELECT order_id, user_id, product_id, product_name, order_status,
                       paid_amount, paid_at, delivered_at, aftersale_deadline
                FROM orders
                WHERE user_id = ?
                ORDER BY paid_at DESC, order_id ASC
                """, (resultSet, rowNumber) -> mapOrder(resultSet), userId);
    }

    private Order mapOrder(ResultSet resultSet) throws SQLException {
        String orderId = resultSet.getString("order_id");
        String productId = resultSet.getString("product_id");
        String productName = resultSet.getString("product_name");
        OrderStatus orderStatus = OrderStatus.valueOf(resultSet.getString("order_status"));
        BigDecimal paidAmount = resultSet.getBigDecimal("paid_amount");
        List<OrderItem> orderItems = findOrderItems(orderId, orderStatus);
        return new Order(
                orderId,
                resultSet.getString("user_id"),
                productId,
                productName,
                orderStatus,
                paidAmount,
                instant(resultSet.getTimestamp("paid_at")),
                nullableInstant(resultSet.getTimestamp("delivered_at")),
                instant(resultSet.getTimestamp("aftersale_deadline")),
                orderItemsOrFallback(orderId, productId, productName, paidAmount, orderStatus, orderItems));
    }

    private List<OrderItem> findOrderItems(String orderId, OrderStatus orderStatus) {
        return jdbcTemplate.query("""
                SELECT oi.order_item_id,
                       oi.product_id,
                       COALESCE(p.product_name, oi.product_name) AS product_name,
                       COALESCE(p.category, oi.category) AS category,
                       oi.quantity,
                       oi.unit_price
                FROM order_items oi
                LEFT JOIN products p ON p.product_id = oi.product_id
                WHERE oi.order_id = ?
                ORDER BY oi.order_item_id ASC
                """, (resultSet, rowNumber) -> mapOrderItem(resultSet, orderStatus), orderId);
    }

    private static OrderItem mapOrderItem(ResultSet resultSet, OrderStatus orderStatus) throws SQLException {
        return OrderItem.fromOrderLine(
                resultSet.getString("order_item_id"),
                resultSet.getString("product_id"),
                resultSet.getString("product_name"),
                resultSet.getString("category"),
                resultSet.getInt("quantity"),
                resultSet.getBigDecimal("unit_price"),
                orderStatus);
    }

    private static List<OrderItem> fallbackOrderItem(
            String orderId,
            String productId,
            String productName,
            BigDecimal paidAmount,
            OrderStatus orderStatus) {
        return List.of(OrderItem.fromOrderLine(
                "OI-" + orderId + "-PRIMARY",
                productId,
                productName,
                "N/A",
                1,
                paidAmount,
                orderStatus));
    }

    private static List<OrderItem> orderItemsOrFallback(
            String orderId,
            String productId,
            String productName,
            BigDecimal paidAmount,
            OrderStatus orderStatus,
            List<OrderItem> orderItems) {
        // 兼容旧 seed/schema 中只有订单主表、没有明细行的数据。
        return orderItems.isEmpty()
                ? fallbackOrderItem(orderId, productId, productName, paidAmount, orderStatus)
                : orderItems;
    }

    private static Instant instant(Timestamp value) {
        return value.toInstant();
    }

    private static Instant nullableInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
