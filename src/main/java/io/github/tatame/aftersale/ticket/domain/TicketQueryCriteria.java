package io.github.tatame.aftersale.ticket.domain;

import java.time.Instant;
import java.util.Locale;

public record TicketQueryCriteria(
        int page,
        int size,
        TicketSortField sortField,
        TicketSortDirection sortDirection,
        TicketStatus status,
        String userId,
        String orderId,
        IntentType intentType,
        Instant createdFrom,
        Instant createdTo) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public TicketQueryCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        sortField = sortField == null ? TicketSortField.CREATED_AT : sortField;
        sortDirection = sortDirection == null ? TicketSortDirection.DESC : sortDirection;
        userId = normalize(userId);
        orderId = normalize(orderId);
        if (createdFrom != null && createdTo != null && createdTo.isBefore(createdFrom)) {
            throw new IllegalArgumentException("createdTo must not be before createdFrom");
        }
    }

    public static TicketQueryCriteria defaultCriteria() {
        return new TicketQueryCriteria(
                DEFAULT_PAGE,
                DEFAULT_SIZE,
                TicketSortField.CREATED_AT,
                TicketSortDirection.DESC,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public long offset() {
        return (long) page * size;
    }

    public String sortApiName() {
        return sortField.apiName() + "," + sortDirection.name().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
