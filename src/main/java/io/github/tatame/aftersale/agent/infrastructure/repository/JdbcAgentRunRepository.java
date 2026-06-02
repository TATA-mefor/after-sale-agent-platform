package io.github.tatame.aftersale.agent.infrastructure.repository;

import io.github.tatame.aftersale.agent.domain.AgentRun;
import io.github.tatame.aftersale.agent.domain.AgentRunRepository;
import io.github.tatame.aftersale.agent.domain.AgentRunStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mysql")
public class JdbcAgentRunRepository implements AgentRunRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JdbcTemplate is a Spring-managed infrastructure collaborator.")
    public JdbcAgentRunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AgentRun save(AgentRun agentRun) {
        jdbcTemplate.update("""
                INSERT INTO agent_runs (
                    run_id, ticket_id, status, plan_json, final_answer, error_message, started_at, finished_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    ticket_id = VALUES(ticket_id),
                    status = VALUES(status),
                    plan_json = VALUES(plan_json),
                    final_answer = VALUES(final_answer),
                    error_message = VALUES(error_message),
                    started_at = VALUES(started_at),
                    finished_at = VALUES(finished_at)
                """,
                agentRun.getRunId(),
                agentRun.getTicketId(),
                agentRun.getStatus().name(),
                agentRun.getPlanJson(),
                agentRun.getFinalAnswer(),
                agentRun.getErrorMessage(),
                timestamp(agentRun.getStartedAt()),
                nullableTimestamp(agentRun.getFinishedAt()));
        return agentRun;
    }

    @Override
    public Optional<AgentRun> findById(String runId) {
        return jdbcTemplate.query("""
                SELECT run_id, ticket_id, status, plan_json, final_answer, error_message, started_at, finished_at
                FROM agent_runs
                WHERE run_id = ?
                """, (resultSet, rowNumber) -> mapAgentRun(resultSet), runId).stream().findFirst();
    }

    private static AgentRun mapAgentRun(ResultSet resultSet) throws SQLException {
        return AgentRun.restore(
                resultSet.getString("run_id"),
                resultSet.getString("ticket_id"),
                AgentRunStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("plan_json"),
                resultSet.getString("final_answer"),
                resultSet.getString("error_message"),
                instant(resultSet.getTimestamp("started_at")),
                nullableInstant(resultSet.getTimestamp("finished_at")));
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }

    private static Timestamp nullableTimestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value.toInstant();
    }

    private static Instant nullableInstant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
