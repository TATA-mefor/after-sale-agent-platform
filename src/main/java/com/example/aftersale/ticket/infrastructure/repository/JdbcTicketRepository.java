package com.example.aftersale.ticket.infrastructure.repository;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketRepository;
import com.example.aftersale.ticket.domain.TicketStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
/*
如果启用 mysql profile，就用 JdbcTicketRepository.java (line 19)：
这里会执行 INSERT INTO tickets ... ON DUPLICATE KEY UPDATE ...，也就是写 MySQL。
*/
@Repository
@Profile("mysql")
public class JdbcTicketRepository implements TicketRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JdbcTemplate is a Spring-managed infrastructure collaborator.")
    public JdbcTicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Ticket save(Ticket ticket) {
        jdbcTemplate.update("""
                INSERT INTO tickets (
                    ticket_id, user_id, order_id, raw_user_message, intent_type, priority, status,
                    internal_note, agent_suggestion, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    user_id = VALUES(user_id),
                    order_id = VALUES(order_id),
                    raw_user_message = VALUES(raw_user_message),
                    intent_type = VALUES(intent_type),
                    priority = VALUES(priority),
                    status = VALUES(status),
                    internal_note = VALUES(internal_note),
                    agent_suggestion = VALUES(agent_suggestion),
                    created_at = VALUES(created_at),
                    updated_at = VALUES(updated_at)
                """,
                ticket.getTicketId(),
                ticket.getUserId(),
                ticket.getOrderId(),
                ticket.getRawUserMessage(),
                ticket.getIntentType().name(),
                ticket.getPriority(),
                ticket.getStatus().name(),
                ticket.getInternalNote(),
                ticket.getAgentSuggestion(),
                timestamp(ticket.getCreatedAt()),
                timestamp(ticket.getUpdatedAt()));
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(String ticketId) {
        return jdbcTemplate.query("""
                SELECT ticket_id, user_id, order_id, raw_user_message, intent_type, priority, status,
                       internal_note, agent_suggestion, created_at, updated_at
                FROM tickets
                WHERE ticket_id = ?
                """, (resultSet, rowNumber) -> mapTicket(resultSet), ticketId).stream().findFirst();
    }

    private static Ticket mapTicket(ResultSet resultSet) throws SQLException {
        return Ticket.restore(
                resultSet.getString("ticket_id"),
                resultSet.getString("user_id"),
                resultSet.getString("order_id"),
                resultSet.getString("raw_user_message"),
                IntentType.valueOf(resultSet.getString("intent_type")),
                resultSet.getString("priority"),
                TicketStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("internal_note"),
                resultSet.getString("agent_suggestion"),
                instant(resultSet.getTimestamp("created_at")),
                instant(resultSet.getTimestamp("updated_at")));
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value.toInstant();
    }
}
