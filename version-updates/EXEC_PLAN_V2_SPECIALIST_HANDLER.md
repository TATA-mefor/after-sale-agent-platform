# EXEC_PLAN_V2_SPECIALIST_HANDLER

Date: 2026-05-16

## Scope

This record closes V2.4: Specialist Agent Handler.

The task adds strategy-class handler dispatch for V2.3 multi-intent subtasks. It does not add real databases,
multi-Agent microservices, queues, parallel execution, voting consensus, real refunds, real exchanges, coupon
compensation, real logistics, or real payment integration.

## What Changed

- Added `SpecialistAgentHandler`.
- Added `SpecialistAgentHandlerRegistry`.
- Added `SubtaskExecutionContext`.
- Added `SubtaskExecutionResult`.
- Added `ReturnAgentHandler`.
- Added `ExchangeAgentHandler`.
- Added `CouponAgentHandler`.
- Added `LogisticsAgentHandler`.
- Added `GeneralConsultationHandler`.
- Added `HumanEscalationHandler`.
- Updated `AgentApplicationService` to dispatch multi-intent subtasks through the registry.
- Kept single-intent planned tool execution unchanged.
- Kept all handler tool calls behind `ToolRegistry`.
- Kept `ToolCallTrace` behavior and subtask metadata in tool input JSON.
- Added ArchUnit rules preventing handler dependencies on repositories, LLM infrastructure, and Spring Web.
- Added handler registry, handler tool-call, unsupported subtask, trace, and regression tests.

## Specialist Handler Flow

```text
Ticket
→ AgentPlanner
→ AgentPlan with subtasks
→ AgentPlanValidator
→ AgentApplicationService
→ SpecialistAgentHandlerRegistry
→ SpecialistAgentHandler
→ ToolRegistry
→ ToolCallTrace
→ SubtaskExecutionResult
→ final summary
```

## Test Coverage

Covered:

- Registry finds the correct handler by `SubtaskType`.
- Registry rejects duplicate handler coverage.
- Unsupported `SubtaskType` returns a structured failed `SubtaskExecutionResult`.
- `ReturnAgentHandler` calls `get_order_by_id`, `search_aftersale_policy`, and `add_ticket_note`.
- `ExchangeAgentHandler` calls `get_order_by_id`, `search_aftersale_policy`, and `add_ticket_note`.
- `CouponAgentHandler` calls `search_aftersale_policy` and `add_ticket_note`.
- Multi-intent AgentRun still executes subtasks sequentially and records trace metadata.
- Single-intent V1/V2.2 AgentRun flow still passes.
- Handler classes do not access repositories, LLM infrastructure, or Spring Web.
- Default tests do not depend on real LLM, API keys, or external network.

## Known Limitations

- Handler execution is still single-process and sequential.
- ToolCallTrace remains a flat list; Execution Tree is V2.7.
- Unsupported but otherwise valid subtask types return structured failure results rather than being auto-routed to an
  invented business action.
- Human escalation is represented as a handler and structured result path, not a full approval operator workflow.
- No real refund, exchange, coupon compensation, logistics, payment, or database integration is implemented.

## Validation

Required commands:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Final results are recorded in the Review Packet for this task.

## Completion Signal

TASK_COMPLETE
