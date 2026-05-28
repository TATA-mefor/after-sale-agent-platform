package com.example.aftersale.ticket.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating a demo after-sale ticket.")
public record TicketCreateRequest(
        @Schema(description = "Demo user id. Use synthetic data only.", example = "U-DEMO-1001")
        String userId,
        @Schema(description = "Demo order id. No live order system is contacted.", example = "O-DEMO-2001")
        String orderId,
        @Schema(description = "Customer after-sale message.", example = "The jacket size is not suitable.")
        String message) {
    /*
    {
        "userId": "U-1001",
        "orderId": "O-2001",
        "message": "我要退货"
    }
    */
}
