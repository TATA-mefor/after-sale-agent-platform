package io.github.tatame.aftersale.ticket.application;

import io.github.tatame.aftersale.common.exception.ResourceNotFoundException;
import io.github.tatame.aftersale.common.observability.MdcScope;
import io.github.tatame.aftersale.common.observability.ObservabilityConstants;
import io.github.tatame.aftersale.ticket.domain.IntentType;
import io.github.tatame.aftersale.ticket.domain.Ticket;
import io.github.tatame.aftersale.ticket.domain.TicketPage;
import io.github.tatame.aftersale.ticket.domain.TicketQueryCriteria;
import io.github.tatame.aftersale.ticket.domain.TicketRepository;
import io.github.tatame.aftersale.ticket.domain.TicketSortDirection;
import io.github.tatame.aftersale.ticket.domain.TicketSortField;
import io.github.tatame.aftersale.ticket.domain.TicketStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 管理工单创建、意图分类、备注和状态流转。
 *
 * <p>边界：本服务是 Ticket 领域状态变更入口；Controller、Agent 和工具都应通过它修改工单，
 * 而不是直接访问 TicketRepository 或在外层重写状态规则。
 */
@Service
public class TicketApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TicketApplicationService.class);

    private final TicketRepository ticketRepository;

    public TicketApplicationService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public Ticket createTicket(String userId, String orderId, String rawUserMessage) {
        String ticketId = "T-" + UUID.randomUUID();
        Ticket ticket = Ticket.create(ticketId, userId, orderId, rawUserMessage, Instant.now());
        Ticket saved = ticketRepository.save(ticket);
        try (MdcScope ignored = MdcScope.put(ObservabilityConstants.TICKET_ID, saved.getTicketId())) {
            LOGGER.info("ticket.created orderId={} status={}", saved.getOrderId(), saved.getStatus());
        }
        return saved;
    }

    public Ticket getTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TICKET_NOT_FOUND",
                        "Ticket not found: " + ticketId));
    }

    public TicketPage listTickets(
            Integer page,
            Integer size,
            String sort,
            TicketStatus status,
            String userId,
            String orderId,
            IntentType intentType,
            Instant createdFrom,
            Instant createdTo) {
        TicketQueryCriteria criteria = new TicketQueryCriteria(
                page == null ? TicketQueryCriteria.DEFAULT_PAGE : page,
                size == null ? TicketQueryCriteria.DEFAULT_SIZE : size,
                parseSortField(sort),
                parseSortDirection(sort),
                status,
                userId,
                orderId,
                intentType,
                createdFrom,
                createdTo);
        return ticketRepository.findPage(criteria);
    }

    public Ticket addTicketNote(String ticketId, String note) {
        Ticket ticket = getTicket(ticketId);
        ticket.addInternalNote(note, Instant.now());
        return ticketRepository.save(ticket);
    }

    public Ticket classifyTicketIntent(String ticketId, IntentType intentType) {
        Ticket ticket = getTicket(ticketId);
        ticket.classifyIntent(intentType, Instant.now());
        Ticket saved = ticketRepository.save(ticket);
        try (MdcScope ignored = MdcScope.put(ObservabilityConstants.TICKET_ID, saved.getTicketId())) {
            LOGGER.info("ticket.intent_classified intentType={}", saved.getIntentType());
        }
        return saved;
    }

    /**
     * 按领域规则推进 Ticket 状态，并在需要原因的终态上强制提供说明。
     */
    public Ticket updateTicketStatus(String ticketId, TicketStatus targetStatus, String reason) {
        Ticket ticket = getTicket(ticketId);
        TicketStatus previousStatus = ticket.getStatus();
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
        Ticket saved = ticketRepository.save(ticket);
        try (MdcScope ignored = MdcScope.putAll(Map.of(
                ObservabilityConstants.TICKET_ID, saved.getTicketId()))) {
            LOGGER.info("ticket.status_updated fromStatus={} toStatus={}", previousStatus, saved.getStatus());
        }
        return saved;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static TicketSortField parseSortField(String sort) {
        return TicketSortField.fromApiName(sortPart(sort, 0));
    }

    private static TicketSortDirection parseSortDirection(String sort) {
        return TicketSortDirection.fromApiName(sortPart(sort, 1));
    }

    private static String sortPart(String sort, int index) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        String[] parts = sort.split(",", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("sort must use the format field,direction");
        }
        if (index >= parts.length) {
            return null;
        }
        return parts[index].trim();
    }
}
