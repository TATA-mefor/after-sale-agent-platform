package io.github.tatame.aftersale.trace.infrastructure.repository;

import io.github.tatame.aftersale.trace.domain.ToolCallStatus;
import io.github.tatame.aftersale.trace.domain.ToolCallTrace;
import io.github.tatame.aftersale.trace.domain.ToolCallTraceRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mysql")
public class JdbcToolCallTraceRepository implements ToolCallTraceRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JdbcTemplate is a Spring-managed infrastructure collaborator.")
    public JdbcToolCallTraceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ToolCallTrace save(ToolCallTrace trace) {
        jdbcTemplate.update("""
                INSERT INTO tool_call_traces (
                    trace_id, run_id, tool_name, input_json, status, output_json,
                    latency_ms, error_message, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    run_id = VALUES(run_id),
                    tool_name = VALUES(tool_name),
                    input_json = VALUES(input_json),
                    status = VALUES(status),
                    output_json = VALUES(output_json),
                    latency_ms = VALUES(latency_ms),
                    error_message = VALUES(error_message),
                    created_at = VALUES(created_at)
                """,
                trace.getTraceId(),
                trace.getRunId(),
                trace.getToolName(),
                trace.getInputJson(),
                trace.getStatus().name(),
                trace.getOutputJson(),
                trace.getLatencyMs(),
                trace.getErrorMessage(),
                timestamp(trace.getCreatedAt()));
        return trace;
    }

    @Override
    public List<ToolCallTrace> findByRunId(String runId) {
        return jdbcTemplate.query("""
                SELECT trace_id, run_id, tool_name, input_json, status, output_json,
                       latency_ms, error_message, created_at
                FROM tool_call_traces
                WHERE run_id = ?
                ORDER BY created_at ASC, trace_id ASC
                """, (resultSet, rowNumber) -> mapTrace(resultSet), runId);
    }

    private static ToolCallTrace mapTrace(ResultSet resultSet) throws SQLException {
        return ToolCallTrace.restore(
                resultSet.getString("trace_id"),
                resultSet.getString("run_id"),
                resultSet.getString("tool_name"),
                resultSet.getString("input_json"),
                ToolCallStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("output_json"),
                resultSet.getLong("latency_ms"),
                resultSet.getString("error_message"),
                instant(resultSet.getTimestamp("created_at")));
    }

    private static Timestamp timestamp(Instant value) {
        return Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value.toInstant();
    }
}
