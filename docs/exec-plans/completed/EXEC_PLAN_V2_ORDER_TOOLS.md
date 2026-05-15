# EXEC_PLAN_V2_ORDER_TOOLS

Date: 2026-05-15

## Scope

This record closes V2.2: Order Query Tools.

The task adds low-risk order fact tools backed by in-memory demo data. It does not expand into real order center
integration, real logistics, real payment, real refunds, real persistence, vector retrieval, approval APIs, or multi-Agent
orchestration.

## What Changed

- Added `OrderRepository`.
- Added `OrderApplicationService`.
- Added in-memory demo order data.
- Added `get_order_by_id`.
- Added `get_user_orders`.
- Registered order tools through Spring and `ToolRegistry`.
- Updated `RuleBasedAgentPlanner` to plan `get_order_by_id`.
- Updated `AgentApplicationService` to execute `get_order_by_id` through `ToolRegistry`.
- Kept `ToolCallTrace` recording unchanged.
- Updated README, tool contracts, risk policy, LLM Planner Contract, and `EXEC_PLAN_V2.md`.

## Demo Data

The in-memory demo orders cover:

- paid but not shipped;
- delivered and still inside the after-sale window;
- delivered but outside the after-sale window;
- logistics exception;
- special/customized goods.

## Order Tool Flow

```text
AgentPlan
→ AgentApplicationService
→ ToolRegistry
→ GetOrderByIdTool / GetUserOrdersTool
→ OrderApplicationService
→ InMemoryOrderRepository
→ ToolCallTrace
```

Tools remain `LOW` risk and read-only. They do not mutate order state.

## Validation

Required commands:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Final results are recorded in the Review Packet for this task.

## Known Limitations

- Order data is in-memory demo data.
- No real order center integration.
- No real logistics or payment integration.
- No persistence across application restarts.
- LLM mode can plan order tools, but default tests still use rule/fake paths and do not require a real LLM.

## Completion Signal

TASK_COMPLETE
