# Agent Risk Policy

M5 enforces risk at the tool boundary.

## LOW

Allowed to execute directly when inputs are valid.

Examples:

- `create_aftersale_ticket`
- `add_ticket_note`

## MEDIUM

Allowed to execute directly only when the requested action stays within the ticket lifecycle and does not close a
dispute or affect funds, inventory, or user rights.

Example:

- `update_ticket_status`

`update_ticket_status` returns `REQUIRES_APPROVAL` when the requested status is `CLOSED`.

## HIGH

Must return `REQUIRES_APPROVAL` before the business executor runs.

Examples:

- refund
- compensation
- payment status change
- dispute closure

M5 does not implement real refund, compensation, payment, inventory, or database-side approval execution.
