package io.github.tatame.aftersale.ticket.domain;

import java.util.Locale;

public enum TicketSortDirection {
    ASC,
    DESC;

    public static TicketSortDirection fromApiName(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }
        try {
            return TicketSortDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported ticket sort direction: " + value, exception);
        }
    }
}
