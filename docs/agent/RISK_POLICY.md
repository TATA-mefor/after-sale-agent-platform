# Agent Risk Policy

M5 enforces risk at the tool boundary.

## LOW

Allowed to execute directly when inputs are valid.

Examples:

- `create_aftersale_ticket`
- `add_ticket_note`
- `search_aftersale_policy`

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

## Final V1 Boundary

V1 does not execute any real financial, inventory, logistics, or order-core mutation. The implemented Agent demo only
executes low-risk policy retrieval and ticket-note writing. Closing a ticket through `update_ticket_status` is blocked
with `REQUIRES_APPROVAL`.
