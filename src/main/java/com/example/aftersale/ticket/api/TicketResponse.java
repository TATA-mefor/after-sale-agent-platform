package com.example.aftersale.ticket.api;

import com.example.aftersale.ticket.domain.IntentType;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketStatus;
import java.time.Instant;

public record TicketResponse(
        String ticketId,
        String userId,
        String orderId,
        String rawUserMessage,
        IntentType intentType,
        String priority,
        TicketStatus status,
        String internalNote,
        String agentSuggestion,
        Instant createdAt,
        Instant updatedAt) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getTicketId(),
                ticket.getUserId(),
                ticket.getOrderId(),
                ticket.getRawUserMessage(),
                ticket.getIntentType(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getInternalNote(),
                ticket.getAgentSuggestion(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
