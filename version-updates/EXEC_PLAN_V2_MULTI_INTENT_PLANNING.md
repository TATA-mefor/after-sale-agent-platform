# EXEC_PLAN_V2_MULTI_INTENT_PLANNING

Date: 2026-05-16

## Scope

This record closes V2.3: Multi-Intent Planning.

The task adds structured subtask planning for complex after-sale tickets. It does not add real databases, multi-Agent
microservices, queues, parallel execution, voting consensus, real refunds, real exchanges, coupon compensation, real
logistics, or real payment integration.

## What Changed

- Extended `AgentPlan` with `subtasks`.
- Added `AgentSubtask`.
- Added `SubtaskType`.
- Added `SubtaskStatus`.
- Updated `AgentPlanParser` to parse subtask JSON.
- Updated `AgentPlanValidator` to validate subtask IDs, types, risk levels, policy queries, planned tools, dependency
  references, dependency cycles, and subtask count.
- Updated `RuleBasedAgentPlanner` to split return + exchange + coupon consultation messages into multiple subtasks.
- Updated `AgentApplicationService` to execute subtasks sequentially by priority.
- Kept all tool execution behind `ToolRegistry`.
- Kept `ToolCallTrace` as a trace list and added subtask metadata to tool input JSON.
- Added parser, validator, planner, flow, trace, and regression tests.

## Multi-Intent Flow

```text
Ticket
→ AgentPlanner
→ AgentPlan with AgentSubtasks
→ AgentPlanValidator
→ AgentApplicationService
→ sequential subtask execution
→ ToolRegistry
→ ToolCallTrace
→ final multi-subtask summary
```

## Test Coverage

Covered:

- Parsing legal subtasks.
- Rejecting unknown subtask type.
- Rejecting unknown planned tools.
- Rejecting blank subtask policy query.
- Rejecting missing dependency subtask ID.
- Rejecting cyclic dependencies.
- Rule-based splitting of complex after-sale messages.
- Sequential subtask execution through ToolRegistry.
- Trace input JSON with subtask metadata.
- Multi-intent final suggestion summary.
- Existing single-intent V1/V2.2 flow.
- Offline default test path.
- ArchitectureTest.

## Known Limitations

- Rule-based multi-intent detection is intentionally narrow.
- ToolCallTrace remains a flat list; Execution Tree is V2.7.
- Specialist handlers are not implemented in V2.3.
- Subtasks are part of AgentRun plan JSON and trace metadata, not a persisted relational model.
- Coupon handling is consultation only; no real coupon system or compensation is implemented.

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
