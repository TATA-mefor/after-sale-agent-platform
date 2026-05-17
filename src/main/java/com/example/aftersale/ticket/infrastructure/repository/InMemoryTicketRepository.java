package com.example.aftersale.ticket.infrastructure.repository;

import com.example.aftersale.ticket.domain.Ticket;
import com.example.aftersale.ticket.domain.TicketRepository;
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
}
