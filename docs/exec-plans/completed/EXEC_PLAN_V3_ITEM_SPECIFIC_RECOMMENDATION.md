# EXEC_PLAN_V3_ITEM_SPECIFIC_RECOMMENDATION

Status: completed

Completion Date: 2026-05-18

## Goal

Use `get_order_by_id` `orderItems` inside Return and Exchange specialist handlers so AgentRun summaries and Ticket notes
can include product-line-level after-sale recommendations without changing the Agent execution boundary.

## What Completed

- Added `OrderItemFact` for AgentWorkspace item facts.
- Extended `OrderFact` to parse `orderItems` from order tool output.
- Added deterministic item matching and recommendation support.
- Updated `ReturnAgentHandler` to generate item-level return recommendations.
- Updated `ExchangeAgentHandler` to generate item-level exchange recommendations.
- Added item-level recommendation text to subtask summaries and Ticket notes.
- Preserved ToolRegistry as the only order-data access path for handlers.
- Added tests for return, exchange, unsupported support flags, special items, fallback matching, Ticket note content,
  final summary content, and ToolCallTrace non-regression.

## Item-Specific Recommendation Flow

```text
Return / Exchange subtask
→ SpecialistAgentHandler
→ ToolRegistry
→ get_order_by_id
→ ToolOutput.data.orderItems
→ AgentWorkspace OrderFact / OrderItemFact
→ deterministic item matcher
→ item-level recommendation
→ SubtaskExecutionResult summary
→ add_ticket_note
→ AgentRun final summary
```

## Matching Rules

- Match by `productName` in subtask target, subtask message fragment, or raw ticket message.
- Match by `category` when product name is not found.
- Match broad clothing words such as `裙子`, `衣服`, `上衣`, `裤子`, `服装`, and `尺码` against clothing categories.
- If no match is found, select the first order item and state the fallback reason.

## Support Flag Boundary

The current MySQL `products` and `order_items` tables do not contain `support_return`, `support_exchange`, or
`is_special_item` columns. V3.7 does not change that schema.

The Java order output model continues to derive:

- `supportReturn`
- `supportExchange`
- `isSpecialItem`

from current demo product and category fields. Ordinary clothing or non-special demo categories default to return and
exchange support. Special/custom goods are marked as restricted by deterministic demo rules.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Recommendations are deterministic demo guidance, not final business decisions.
- No real refund, exchange, inventory, logistics, payment, coupon, or order-center integration is connected.
- Item matching is intentionally simple and keyword-based.
- Support flags are Java-derived demo metadata, not persisted columns in MySQL.
- Execution Tree and ToolCallTrace were not structurally refactored.

## Follow-ups

- Add curated multi-item evaluation cases after the recommendation wording stabilizes.
- Consider policy-specific item restrictions only after a separate policy data design.
- Consider richer item matching only if deterministic rules become insufficient.

## Completion Signal

TASK_COMPLETE
