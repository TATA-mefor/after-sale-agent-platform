package io.github.tatame.aftersale.ticket.domain;

import java.util.Arrays;

public enum TicketSortField {
    CREATED_AT("createdAt", "created_at"),
    UPDATED_AT("updatedAt", "updated_at"),
    TICKET_ID("ticketId", "ticket_id");

    private final String apiName;
    private final String persistenceColumn;

    TicketSortField(String apiName, String persistenceColumn) {
        this.apiName = apiName;
        this.persistenceColumn = persistenceColumn;
    }

    public String apiName() {
        return apiName;
    }

    public String persistenceColumn() {
        return persistenceColumn;
    }

    public static TicketSortField fromApiName(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT;
        }
        return Arrays.stream(values())
                .filter(field -> field.apiName.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported ticket sort field: " + value));
    }
}
