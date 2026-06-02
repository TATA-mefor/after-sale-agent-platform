# EXEC_PLAN_V3_ORDER_ITEMS_TOOL_ENRICHMENT

Status: completed

Completion Date: 2026-05-18

## Goal

Expose V3.5 `products` and `order_items` demo data through existing order query tools so Agent execution can use order
header facts plus product-level item details without changing Agent execution semantics or default offline tests.

## What Completed

- Added a pure domain `OrderItem` model.
- Extended `Order` with structured `orderItems`.
- Preserved the existing `Order` constructor through a fallback primary item.
- Updated the default in-memory order repository with item-level demo data.
- Updated the MySQL order repository to query `order_items` joined with `products`.
- Added a MySQL fallback primary item for legacy local databases without item rows.
- Added `orderItems` to `get_order_by_id` tool output.
- Reused the shared order output mapper for `get_user_orders`.
- Extended `OrderFact` with an item summary for AgentWorkspace context.
- Added tests for ToolRegistry output, AgentRun trace output, special-item flags, and schema/seed harness checks.
- Updated README, `EXEC_PLAN_V3.md`, dataset mapping notes, and quality score docs.

## Order Items Flow

```text
get_order_by_id
→ ToolRegistry
→ GetOrderByIdTool
→ OrderApplicationService
→ OrderRepository
→ InMemoryOrderRepository or JdbcOrderRepository
→ Order + OrderItem list
→ ToolOutput.data.orderItems
→ ToolCallTrace.outputJson
→ Execution Tree inspection
```

In the explicit `mysql` profile, `JdbcOrderRepository` reads:

```text
orders
→ order_items
→ products
```

In the default profile, `InMemoryOrderRepository` returns equivalent demo item data and requires no MySQL, Docker, raw
datasets, real LLM, API key, or external network.

## Tool Output Shape

Each `orderItems` entry includes:

- `orderItemId`
- `productId`
- `productName`
- `category`
- `quantity`
- `unitPrice`
- `itemStatus`
- `supportReturn`
- `supportExchange`
- `isSpecialItem`

Special or custom-category demo goods are marked as not supporting return/exchange. This is only deterministic demo
policy metadata and does not execute any refund, exchange, inventory, logistics, or coupon action.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Product and order-item data remains demo data.
- `get_user_orders` may include item lists through the shared mapper, but it remains a lightweight lookup.
- No production order center is connected.
- No real refund, exchange, payment, logistics, inventory, or coupon system is connected.
- Execution Tree and ToolCallTrace were not structurally refactored; they expose `orderItems` through existing
  serialized tool output.

## Follow-ups

- Consider a dedicated product detail API only if a later workflow needs direct product lookup outside order tools.
- Consider curated multi-item evaluation cases after demo generated cases are reviewed.
- Consider explicit MySQL integration tests behind an opt-in profile without changing the default offline test path.

## Completion Signal

TASK_COMPLETE
