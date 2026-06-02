# EXEC_PLAN_V2_LLM_INTEGRATION

Date: 2026-05-14

## Scope

This record closes V2.1, V2.1.1, and V2.1.2: LLM Planner Adapter, real provider-call boundary, structured AgentPlan
parsing, and an explicit opt-in live smoke test.

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
- Added `LlmClient`, `LlmRequest`, and `LlmResponse`.
- Added a lightweight OpenAI-compatible Responses client.
- Added centralized planner prompt construction.
- Added structured AgentPlan JSON parsing.
- Added AgentPlan validation for enum values, required fields, registered tools, and unsafe completion claims.
- Added `LlmPlannerLiveSmokeTest` as an explicit opt-in smoke test for real provider calls.
- Added `agent.planner.mode=rule|fake|llm`.
- Kept default mode as `rule`.
- Refactored `AgentApplicationService` to depend on `AgentPlanner` and continue executing tools through `ToolRegistry`.
- Kept `ToolCallTrace` behavior in the tool execution path.

## LLM Boundary

V2.1.1 can perform a real LLM provider call only when `agent.planner.mode=llm` is explicitly selected and a valid API Key
is provided through environment or local configuration.

The `LlmAgentPlanner` currently:

- builds a centralized planner prompt;
- calls `LlmClient`;
- parses the raw LLM JSON output into `AgentPlan`;
- validates the plan before any tool execution;
- fails clearly when `agent.planner.mode=llm` has no API Key;
- never executes tools directly.

All tool execution remains in `AgentApplicationService` through `ToolRegistry`, and trace recording remains unchanged.
Default tests use rule/fake planners or a fake `LlmClient`; they do not use real network, real API Keys, or paid LLM
calls.

## Live Smoke Test

Manual command:

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
```

Required local environment:

```text
OPENAI_API_KEY
```

Optional local environment:

```text
AFTERSALE_LLM_MODEL
OPENAI_RESPONSES_ENDPOINT
```

Behavior:

- without `-Dlive.llm=true`, the test is disabled;
- with `-Dlive.llm=true` but no `OPENAI_API_KEY`, the test is skipped with a clear message;
- with complete local configuration, the test calls `LlmAgentPlanner` through `OpenAiLlmClient`;
- the test validates that the returned content parses into `AgentPlan` and passes `AgentPlanValidator`;
- the test does not create a ticket, create an `AgentRun`, write `ToolCallTrace`, or execute business tools.

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

- Real LLM calls are not exercised by automated tests.
- The provider client is intentionally lightweight and currently targets an OpenAI-compatible Responses endpoint.
- Prompt regression tests remain future work.
- Live provider smoke testing is manual and depends on the developer's local API Key and network.
- Order query tools are handled by the separate V2.2 execution record.
- MySQL persistence remains V2.3.
- Approval APIs remain V2.4.

## Completion Signal

TASK_COMPLETE
