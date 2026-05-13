package com.example.aftersale.ticket.application;

import com.example.aftersale.common.exception.ResourceNotFoundException;
import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketRepository;
import com.example.aftersale.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TicketApplicationService {

    private final TicketRepository ticketRepository;

    public TicketApplicationService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket createTicket(String userId, String orderId, String rawUserMessage) {
        String ticketId = "T-" + UUID.randomUUID();
        Ticket ticket = Ticket.create(ticketId, userId, orderId, rawUserMessage, Instant.now());
        return ticketRepository.save(ticket);
    }

    public Ticket getTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TICKET_NOT_FOUND",
                        "Ticket not found: " + ticketId));
    }

    public Ticket addTicketNote(String ticketId, String note) {
        Ticket ticket = getTicket(ticketId);
        ticket.addInternalNote(note, Instant.now());
        return ticketRepository.save(ticket);
    }

    public Ticket updateTicketStatus(String ticketId, TicketStatus targetStatus, String reason) {
        Ticket ticket = getTicket(ticketId);
        Instant changedAt = Instant.now();
        switch (Objects.requireNonNull(targetStatus, "targetStatus must not be null")) {
            case AGENT_RUNNING -> ticket.startAgentRun(changedAt);
            case WAITING_USER_INFO -> ticket.waitForUserInfo(changedAt);
            case WAITING_HUMAN_APPROVAL -> ticket.waitForHumanApproval(changedAt);
            case PROCESSING -> ticket.startProcessing(changedAt);
            case RESOLVED -> ticket.resolve(requireText(reason, "reason"), changedAt);
            case REJECTED -> ticket.reject(requireText(reason, "reason"), changedAt);
            case FAILED -> ticket.fail(requireText(reason, "reason"), changedAt);
            case CLOSED -> ticket.close(changedAt);
            case CREATED -> throw new IllegalArgumentException("Cannot transition ticket back to CREATED");
        }
        return ticketRepository.save(ticket);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
