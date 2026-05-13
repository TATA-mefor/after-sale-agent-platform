package com.example.aftersale.ticket.domain;

import java.util.Optional;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(String ticketId);
}
