package io.github.tatame.aftersale.ticket.api;

import io.github.tatame.aftersale.ticket.domain.TicketPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "Paged ticket list response. This is read-only and does not run an Agent.")
public record TicketPageResponse(
        @Schema(description = "Ticket items for the requested page.")
        List<TicketResponse> items,
        @Schema(description = "Zero-based page index.", example = "0")
        int page,
        @Schema(description = "Requested page size. Maximum is 100.", example = "20")
        int size,
        @Schema(description = "Total matching tickets.", example = "42")
        long totalElements,
        @Schema(description = "Total available pages.", example = "3")
        int totalPages,
        @Schema(description = "Whether another page exists.", example = "true")
        boolean hasNext,
        @Schema(description = "Whether a previous page exists.", example = "false")
        boolean hasPrevious,
        @Schema(description = "Applied sort in field,direction format.", example = "createdAt,desc")
        String sort) {

    public TicketPageResponse {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }

    public static TicketPageResponse from(TicketPage page) {
        return new TicketPageResponse(
                page.items().stream().map(TicketResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.sort());
    }
}
