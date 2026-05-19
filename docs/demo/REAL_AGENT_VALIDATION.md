# Real Agent Validation

This guide describes the V3.9 opt-in live validation path for running a real LLM Planner against local MySQL seed data
through the existing HTTP APIs.

## What It Validates

`RealAgentValidationLiveTest` starts the Spring application with:

```text
spring.profiles.active=mysql
agent.planner.mode=llm
```

It then exercises:

```text
POST /api/tickets
POST /api/tickets/{ticketId}/agent-runs
GET /api/agent-runs/{runId}/execution-tree
GET /api/agent-runs/{runId}/traces
```

The expected internal chain is:

```text
Ticket API
-> LlmAgentPlanner
-> Context Budget / PromptUsageTelemetry
-> AgentPlanValidator
-> Specialist Handler
-> ToolRegistry
-> get_order_by_id
-> search_aftersale_policy
-> add_ticket_note
-> ToolCallTrace
-> Execution Tree
```

## Required Local Setup

Prepare a local MySQL database with the V3.1 schema and seed data:

```text
schema-mysql.sql
data-mysql.sql
```

The default live order id is:

```text
O202605130001
```

You can override it with `AFTERSALE_LIVE_ORDER_ID` if your local seed data uses another order.

Set required environment variables locally:

```text
OPENAI_API_KEY
AFTERSALE_MYSQL_URL
AFTERSALE_MYSQL_USERNAME
AFTERSALE_MYSQL_PASSWORD
```

Optional environment variables:

```text
OPENAI_RESPONSES_ENDPOINT
AFTERSALE_LLM_MODEL
AFTERSALE_LIVE_ORDER_ID
```

Do not commit real API keys, database passwords, local absolute paths, or provider account details.

## Run Command

```bash
mvn test -Dtest=RealAgentValidationLiveTest -Dlive.llm=true -Dlive.mysql=true
```

Both live flags are required. Missing either flag disables the test. Missing any required environment variable disables
the test before the MySQL/LLM application context is loaded.

## Expected Assertions

The test asserts:

- Ticket creation succeeds.
- AgentRun creation succeeds.
- `runId` is present.
- Execution Tree is queryable.
- Tool calls are visible.
- `get_order_by_id` appears in trace/tool calls.
- `get_order_by_id` output contains `orderItems`.
- `search_aftersale_policy` appears in trace/tool calls.
- `add_ticket_note` appears in trace/tool calls.
- Trace count is greater than zero.
- Final summary or Execution Tree contains item-level recommendation evidence.

The test does not assert exact LLM prose because provider output can vary.

## Common Failures

Provider/account balance:

- HTTP 403
- `INSUFFICIENT_BALANCE`
- `insufficient balance`
- `insufficient quota`

These indicate provider or billing setup for the explicit live run. They are not default-test failures.

MySQL setup:

- MySQL is not running.
- `AFTERSALE_MYSQL_URL` points to the wrong database.
- Seed data was not imported.
- `AFTERSALE_LIVE_ORDER_ID` does not exist.

LLM output:

- Provider returns non-JSON output.
- AgentPlan parsing fails.
- AgentPlan validation rejects an unknown tool, unsafe claim, invalid subtask, or dependency cycle.
- Prompt budget is exceeded before the provider call.

The implementation must not print full prompts, API keys, database passwords, or sensitive credentials in logs or
assertion output.

## Why Default Tests Do Not Run It

The default validation path remains:

```text
RuleBased / Fake / in-memory / offline / deterministic
```

The real validation path is:

```text
LLM Planner + MySQL seed data + ToolRegistry + Trace + Execution Tree
```

Keeping this path opt-in protects local and CI runs from requiring Docker, MySQL, external network access, provider
credentials, or live-provider account balance.
