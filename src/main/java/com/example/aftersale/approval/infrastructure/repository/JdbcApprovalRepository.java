package com.example.aftersale.approval.infrastructure.repository;

import com.example.aftersale.approval.domain.ApprovalRepository;
import com.example.aftersale.approval.domain.ApprovalRequest;
import com.example.aftersale.approval.domain.ApprovalStatus;
import com.example.aftersale.tool.domain.ToolRiskLevel;
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
public class JdbcApprovalRepository implements ApprovalRepository {

    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JdbcTemplate is a Spring-managed infrastructure collaborator.")
    public JdbcApprovalRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ApprovalRequest save(ApprovalRequest request) {
        jdbcTemplate.update("""
                INSERT INTO approval_requests (
                    approval_id, ticket_id, run_id, subtask_id, tool_name, requested_action,
                    risk_level, status, reviewer_id, decision_reason, requested_at, reviewed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    ticket_id = VALUES(ticket_id),
                    run_id = VALUES(run_id),
                    subtask_id = VALUES(subtask_id),
                    tool_name = VALUES(tool_name),
                    requested_action = VALUES(requested_action),
                    risk_level = VALUES(risk_level),
                    status = VALUES(status),
                    reviewer_id = VALUES(reviewer_id),
                    decision_reason = VALUES(decision_reason),
                    requested_at = VALUES(requested_at),
                    reviewed_at = VALUES(reviewed_at)
                """,
                request.getApprovalId(),
                request.getTicketId(),
                request.getRunId(),
                request.getSubtaskId(),
                request.getToolName(),
                request.getRequestedAction(),
                request.getRiskLevel().name(),
                request.getStatus().name(),
                request.getReviewerId(),
                request.getDecisionReason(),
                timestamp(request.getRequestedAt()),
                nullableTimestamp(request.getReviewedAt()));
        return request;
    }

    @Override
    public Optional<ApprovalRequest> findById(String approvalId) {
        return jdbcTemplate.query("""
                SELECT approval_id, ticket_id, run_id, subtask_id, tool_name, requested_action,
                       risk_level, status, reviewer_id, decision_reason, requested_at, reviewed_at
                FROM approval_requests
                WHERE approval_id = ?
                """, (resultSet, rowNumber) -> mapApproval(resultSet), approvalId).stream().findFirst();
    }

    @Override
    public List<ApprovalRequest> findByStatus(ApprovalStatus status) {
        return jdbcTemplate.query("""
                SELECT approval_id, ticket_id, run_id, subtask_id, tool_name, requested_action,
                       risk_level, status, reviewer_id, decision_reason, requested_at, reviewed_at
                FROM approval_requests
                WHERE status = ?
                ORDER BY requested_at ASC, approval_id ASC
                """, (resultSet, rowNumber) -> mapApproval(resultSet), status.name());
    }

    @Override
    public List<ApprovalRequest> findByRunId(String runId) {
        return jdbcTemplate.query("""
                SELECT approval_id, ticket_id, run_id, subtask_id, tool_name, requested_action,
                       risk_level, status, reviewer_id, decision_reason, requested_at, reviewed_at
                FROM approval_requests
                WHERE run_id = ?
                ORDER BY requested_at ASC, approval_id ASC
                """, (resultSet, rowNumber) -> mapApproval(resultSet), runId);
    }

    private static ApprovalRequest mapApproval(ResultSet resultSet) throws SQLException {
        return ApprovalRequest.restore(
                resultSet.getString("approval_id"),
                resultSet.getString("ticket_id"),
                resultSet.getString("run_id"),
                resultSet.getString("subtask_id"),
                resultSet.getString("tool_name"),
                resultSet.getString("requested_action"),
                ToolRiskLevel.valueOf(resultSet.getString("risk_level")),
                ApprovalStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("reviewer_id"),
                resultSet.getString("decision_reason"),
                instant(resultSet.getTimestamp("requested_at")),
                nullableInstant(resultSet.getTimestamp("reviewed_at")));
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
