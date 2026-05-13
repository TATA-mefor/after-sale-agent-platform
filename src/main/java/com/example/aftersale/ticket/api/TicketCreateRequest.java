package com.example.aftersale.ticket.api;

public record TicketCreateRequest(String userId, String orderId, String message) {
}
