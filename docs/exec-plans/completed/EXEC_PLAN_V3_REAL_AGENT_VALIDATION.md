# EXEC_PLAN_V3_REAL_AGENT_VALIDATION

Status: completed

Completion date: 2026-05-18

## Goal

Add an explicit opt-in validation path that runs a real LLM Planner against local MySQL seed data through the existing
HTTP APIs, without changing the default offline test path.

## Scope Completed

- Added `RealAgentValidationLiveTest`.
- Required both `-Dlive.llm=true` and `-Dlive.mysql=true`.
- Required `OPENAI_API_KEY`, `AFTERSALE_MYSQL_URL`, `AFTERSALE_MYSQL_USERNAME`, and `AFTERSALE_MYSQL_PASSWORD`.
- Used `@ActiveProfiles("mysql")` and `agent.planner.mode=llm` only for the live test.
- Exercised HTTP Ticket creation, HTTP AgentRun creation, HTTP Execution Tree query, and HTTP Trace query.
- Used `O202605130001` as the default MySQL seed order id, with `AFTERSALE_LIVE_ORDER_ID` as an optional override.
- Asserted tool execution visibility through trace/tool calls.
- Asserted `get_order_by_id` output contains `orderItems`.
- Asserted final summary or execution tree contains item-level recommendation evidence.
- Added provider/account-balance failure detection for HTTP 403 and insufficient-balance responses.
- Updated the OpenAI strict JSON schema to include `subtasks`, matching the existing parser and specialist-handler
  contract.
- Added `docs/demo/REAL_AGENT_VALIDATION.md`.
- Updated README, EXEC_PLAN_V3, and QUALITY_SCORE.

## Real Agent Validation Flow

```text
POST /api/tickets
-> POST /api/tickets/{ticketId}/agent-runs
-> LlmAgentPlanner
-> Context Budget / PromptUsageTelemetry
-> AgentPlanValidator
-> Specialist Handler
-> ToolRegistry
-> get_order_by_id
-> search_aftersale_policy
-> add_ticket_note
-> ToolCallTrace
-> GET /api/agent-runs/{runId}/execution-tree
-> GET /api/agent-runs/{runId}/traces
```

## Default-Test Boundary

Default `mvn test` does not run the live chain. The live test is disabled before context startup unless the required
system properties and environment variables are present.

Default path:

```text
RuleBased / Fake / in-memory / offline / deterministic
```

Live path:

```text
LLM Planner + MySQL seed data + ToolRegistry + Trace + Execution Tree
```

## Provider / Balance Handling

The live test fails with a clear provider/account-balance message when the response contains:

- `HTTP 403`
- `INSUFFICIENT_BALANCE`
- `insufficient balance`
- `insufficient quota`

It does not print API keys, database passwords, full prompt text, or sensitive credentials.

## Validation Commands

Default validation:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional live validation:

```bash
mvn test -Dtest=RealAgentValidationLiveTest -Dlive.llm=true -Dlive.mysql=true
```

## Risks

- Real LLM output is non-deterministic, so the test asserts structure and boundary evidence instead of exact long text.
- Local MySQL state must contain the documented seed order or a valid `AFTERSALE_LIVE_ORDER_ID`.
- Provider balance, provider access, endpoint availability, and local network availability can fail explicit live runs.

## Completion Signal

TASK_COMPLETE
