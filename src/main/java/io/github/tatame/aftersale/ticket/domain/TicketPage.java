package io.github.tatame.aftersale.ticket.domain;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record TicketPage(
        List<Ticket> items,
        int page,
        int size,
        long totalElements,
        TicketSortField sortField,
        TicketSortDirection sortDirection) {

    public TicketPage {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must not be negative");
        }
        sortField = Objects.requireNonNull(sortField, "sortField must not be null");
        sortDirection = Objects.requireNonNull(sortDirection, "sortDirection must not be null");
    }

    public int totalPages() {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0 && totalPages() > 0;
    }

    public String sort() {
        return sortField.apiName() + "," + sortDirection.name().toLowerCase(Locale.ROOT);
    }
}
