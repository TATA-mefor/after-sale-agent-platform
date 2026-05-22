package com.example.aftersale.ticket.api;

public record TicketCreateRequest(String userId, String orderId, String message) {
    /*
    {
        "userId": "U-1001",
        "orderId": "O-2001",
        "message": "我要退货"
    }
    */
}
