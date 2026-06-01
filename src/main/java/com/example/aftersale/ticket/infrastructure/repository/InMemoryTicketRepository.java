package com.example.aftersale.ticket.infrastructure.repository;

import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketPage;
import com.example.aftersale.ticket.domain.TicketQueryCriteria;
import com.example.aftersale.ticket.domain.TicketRepository;
import com.example.aftersale.ticket.domain.TicketSortDirection;
import com.example.aftersale.ticket.domain.TicketSortField;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!mysql")
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    @Override
    public Ticket save(Ticket ticket) {
        tickets.put(ticket.getTicketId(), ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(String ticketId) {
        return Optional.ofNullable(tickets.get(ticketId));
    }

    @Override
    public TicketPage findPage(TicketQueryCriteria criteria) {
        List<Ticket> filtered = tickets.values().stream()
                .filter(ticket -> criteria.status() == null || ticket.getStatus() == criteria.status())
                .filter(ticket -> criteria.userId() == null || ticket.getUserId().equals(criteria.userId()))
                .filter(ticket -> criteria.orderId() == null || ticket.getOrderId().equals(criteria.orderId()))
                .filter(ticket -> criteria.intentType() == null || ticket.getIntentType() == criteria.intentType())
                .filter(ticket -> criteria.createdFrom() == null
                        || !ticket.getCreatedAt().isBefore(criteria.createdFrom()))
                .filter(ticket -> criteria.createdTo() == null || !ticket.getCreatedAt().isAfter(criteria.createdTo()))
                .sorted(ticketComparator(criteria.sortField(), criteria.sortDirection()))
                .toList();

        int fromIndex = (int) Math.min(criteria.offset(), filtered.size());
        int toIndex = Math.min(fromIndex + criteria.size(), filtered.size());
        return new TicketPage(
                filtered.subList(fromIndex, toIndex),
                criteria.page(),
                criteria.size(),
                filtered.size(),
                criteria.sortField(),
                criteria.sortDirection());
    }

    private static Comparator<Ticket> ticketComparator(
            TicketSortField sortField,
            TicketSortDirection sortDirection) {
        Comparator<Ticket> fieldComparator = switch (sortField) {
            case CREATED_AT -> Comparator.comparing(Ticket::getCreatedAt);
            case UPDATED_AT -> Comparator.comparing(Ticket::getUpdatedAt);
            case TICKET_ID -> Comparator.comparing(Ticket::getTicketId);
        };
        if (sortDirection == TicketSortDirection.DESC) {
            fieldComparator = fieldComparator.reversed();
        }
        return fieldComparator.thenComparing(Ticket::getTicketId);
    }
}
