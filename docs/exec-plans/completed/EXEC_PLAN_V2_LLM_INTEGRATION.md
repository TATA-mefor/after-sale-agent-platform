# EXEC_PLAN_V2_LLM_INTEGRATION

Date: 2026-05-14

## Scope

This record closes V2.1: LLM Planner Adapter.

The task introduced a Planner boundary without expanding into V2.2 order tools, V2.3 persistence, V2.4 approval APIs,
vector retrieval, real refunds, or multi-Agent orchestration.

## What Changed

- Added `AgentPlanner`.
- Added `AgentPlanningContext`.
- Added `AgentPlan`.
- Added `PlannedToolCall`.
- Added `AgentPlanValidator`.
- Moved V1 rule-based intent and planning behavior into `RuleBasedAgentPlanner`.
- Added `FakeAgentPlanner` for deterministic tests.
- Added `LlmAgentPlanner` boundary and configuration validation.
- Added `agent.planner.mode=rule|fake|llm`.
- Kept default mode as `rule`.
- Refactored `AgentApplicationService` to depend on `AgentPlanner` and continue executing tools through `ToolRegistry`.
- Kept `ToolCallTrace` behavior in the tool execution path.

## LLM Boundary

V2.1 does not perform a real LLM provider call.

The `LlmAgentPlanner` currently:

- reads configured LLM settings;
- fails clearly when `agent.planner.mode=llm` has no API Key;
- throws an explicit TODO for real provider calls.

It does not fake a successful LLM response.

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

- Real LLM provider SDK integration is not implemented.
- LLM response JSON parsing is not implemented.
- Order query tools remain V2.2.
- MySQL persistence remains V2.3.
- Approval APIs remain V2.4.

## Completion Signal

TASK_COMPLETE
