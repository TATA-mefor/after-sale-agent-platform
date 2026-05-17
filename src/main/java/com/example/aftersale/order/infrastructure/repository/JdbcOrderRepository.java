package com.example.aftersale.order.infrastructure.repository;

import com.example.aftersale.order.domain.Order;
import com.example.aftersale.order.domain.OrderRepository;
import com.example.aftersale.order.domain.OrderStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

    private static Order mapOrder(ResultSet resultSet) throws SQLException {
        return new Order(
                resultSet.getString("order_id"),
                resultSet.getString("user_id"),
                resultSet.getString("product_id"),
                resultSet.getString("product_name"),
                OrderStatus.valueOf(resultSet.getString("order_status")),
                resultSet.getBigDecimal("paid_amount"),
                instant(resultSet.getTimestamp("paid_at")),
                nullableInstant(resultSet.getTimestamp("delivered_at")),
                instant(resultSet.getTimestamp("aftersale_deadline")));
    }

    private static Instant instant(Timestamp value) {
        return value.toInstant();
    }

    private static Instant nullableInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
