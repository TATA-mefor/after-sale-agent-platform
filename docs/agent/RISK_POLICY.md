# Agent Risk Policy

M5 enforces risk at the tool boundary.

## LOW

Allowed to execute directly when inputs are valid.

Examples:

- `get_order_by_id`
- `get_user_orders`
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

## V2.2 Boundary

V2.2 adds low-risk order query tools backed by in-memory demo data. They read demo order facts only and do not connect
to a real order center, logistics provider, payment provider, database, refund system, or inventory system.

Closing a ticket through `update_ticket_status` remains blocked with `REQUIRES_APPROVAL`.

## V2.3 Multi-Intent Risk Boundary

V2.3 introduces planned subtasks for complex after-sale messages. Risk is evaluated per subtask and per planned tool.

Rules:

- `LOW` subtasks may plan read-only tools or low-risk note writing;
- `MEDIUM` subtasks may plan controlled ticket lifecycle actions, but not funds, inventory, coupons, payment, or dispute
  closure;
- `HIGH` subtasks must stop at planning or approval handoff and cannot be executed automatically;
- subtask `riskLevel` must be one of `LOW`, `MEDIUM`, or `HIGH`;
- subtask output must not claim that refund, exchange, coupon compensation, payment change, or dispute closure has
  already completed;
- ToolRegistry and the existing tool risk contract remain the execution boundary.

V2.3 does not implement real refund, real exchange, real coupon compensation, real logistics, or real payment actions.
