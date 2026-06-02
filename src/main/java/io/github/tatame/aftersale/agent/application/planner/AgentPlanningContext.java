package io.github.tatame.aftersale.agent.application.planner;

import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AgentPlanningContext(
        String ticketId,
        String userId,
        String orderId,
        String rawUserMessage,
        TicketStatus currentTicketStatus,
        List<String> availableTools,
        String riskPolicySummary,
        Instant createdAt) {

    public AgentPlanningContext {
        ticketId = requireText(ticketId, "ticketId");
        userId = requireText(userId, "userId");
        orderId = requireText(orderId, "orderId");
        rawUserMessage = requireText(rawUserMessage, "rawUserMessage");
        currentTicketStatus = Objects.requireNonNull(currentTicketStatus, "currentTicketStatus must not be null");
        availableTools = List.copyOf(Objects.requireNonNull(availableTools, "availableTools must not be null"));
        riskPolicySummary = requireText(riskPolicySummary, "riskPolicySummary");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
