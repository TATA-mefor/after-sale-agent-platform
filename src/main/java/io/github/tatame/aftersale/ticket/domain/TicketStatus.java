package io.github.tatame.aftersale.ticket.domain;

public enum TicketStatus {
    CREATED,
    AGENT_RUNNING,
    WAITING_USER_INFO,
    WAITING_HUMAN_APPROVAL,
    PROCESSING,
    RESOLVED,
    REJECTED,
    CLOSED,
    FAILED
}
