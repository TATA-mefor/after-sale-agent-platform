package com.example.aftersale.ticket.infrastructure.repository;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketPage;
import com.example.aftersale.ticket.domain.TicketQueryCriteria;
import com.example.aftersale.ticket.domain.TicketRepository;
import com.example.aftersale.ticket.domain.TicketStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

    @Override
    public TicketPage findPage(TicketQueryCriteria criteria) {
        QueryParts where = buildWhere(criteria);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tickets" + where.sql(),
                Long.class,
                where.parameters().toArray());

        List<Object> queryParameters = new ArrayList<>(where.parameters());
        queryParameters.add(criteria.size());
        queryParameters.add(criteria.offset());

        List<Ticket> items = jdbcTemplate.query("""
                SELECT ticket_id, user_id, order_id, raw_user_message, intent_type, priority, status,
                       internal_note, agent_suggestion, created_at, updated_at
                FROM tickets
                """
                        + where.sql()
                        + " ORDER BY " + criteria.sortField().persistenceColumn() + " "
                        + criteria.sortDirection().name()
                        + ", ticket_id ASC LIMIT ? OFFSET ?",
                (resultSet, rowNumber) -> mapTicket(resultSet),
                queryParameters.toArray());

        return new TicketPage(
                items,
                criteria.page(),
                criteria.size(),
                total == null ? 0 : total,
                criteria.sortField(),
                criteria.sortDirection());
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

    private static QueryParts buildWhere(TicketQueryCriteria criteria) {
        List<String> clauses = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        addEquals(clauses, parameters, "status", criteria.status() == null ? null : criteria.status().name());
        addEquals(clauses, parameters, "user_id", criteria.userId());
        addEquals(clauses, parameters, "order_id", criteria.orderId());
        addEquals(
                clauses,
                parameters,
                "intent_type",
                criteria.intentType() == null ? null : criteria.intentType().name());
        if (criteria.createdFrom() != null) {
            clauses.add("created_at >= ?");
            parameters.add(timestamp(criteria.createdFrom()));
        }
        if (criteria.createdTo() != null) {
            clauses.add("created_at <= ?");
            parameters.add(timestamp(criteria.createdTo()));
        }
        if (clauses.isEmpty()) {
            return new QueryParts("", parameters);
        }
        return new QueryParts(" WHERE " + String.join(" AND ", clauses), parameters);
    }

    private static void addEquals(
            List<String> clauses,
            List<Object> parameters,
            String column,
            String value) {
        if (value != null) {
            clauses.add(column + " = ?");
            parameters.add(value);
        }
    }

    private record QueryParts(String sql, List<Object> parameters) {
        private QueryParts {
            parameters = List.copyOf(parameters);
        }
    }
}
