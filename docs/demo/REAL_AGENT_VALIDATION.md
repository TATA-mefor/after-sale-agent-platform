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

Set required environment variables locally for the selected LLM provider:

```text
OPENAI_API_KEY for openai-responses, or DASHSCOPE_API_KEY for dashscope providers
AFTERSALE_MYSQL_URL
AFTERSALE_MYSQL_USERNAME
AFTERSALE_MYSQL_PASSWORD
```

Optional environment variables:

```text
AFTERSALE_LLM_PROVIDER
OPENAI_RESPONSES_ENDPOINT
DASHSCOPE_BASE_URL
DASHSCOPE_RESPONSES_ENDPOINT
DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT
AFTERSALE_LLM_MODEL
AFTERSALE_LIVE_ORDER_ID
SPRING_AI_ENABLED
SPRING_AI_CHAT_ENABLED
SPRING_AI_MODEL_CHAT
SPRING_AI_OPENAI_API_KEY
SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL
```

Do not commit real API keys, database passwords, local absolute paths, or provider account details.

## DashScope / Qwen Provider Examples

DashScope Chat Completions compatible mode:

```powershell
$env:AFTERSALE_LLM_PROVIDER="dashscope-chat-compatible"
$env:DASHSCOPE_API_KEY="你的 DashScope API Key"
$env:DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1"
$env:AFTERSALE_LLM_MODEL="qwen3.6-plus"
```

DashScope Responses compatible mode:

```powershell
$env:AFTERSALE_LLM_PROVIDER="dashscope-responses"
$env:DASHSCOPE_API_KEY="你的 DashScope API Key"
$env:DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1"
$env:AFTERSALE_LLM_MODEL="qwen3.6-plus"
```

For `dashscope-responses`, the default endpoint is `${DASHSCOPE_BASE_URL}/responses`. For
`dashscope-chat-compatible`, the default endpoint is `${DASHSCOPE_BASE_URL}/chat/completions`. You can override them
with `DASHSCOPE_RESPONSES_ENDPOINT` or `DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT`.

## Spring AI Provider Example

Spring AI can be used as the LLM provider adapter for the same live Agent validation path. It remains opt-in and does
not execute project tools through Spring AI tool/function calling.

```powershell
$env:AFTERSALE_LLM_PROVIDER="spring-ai-chat"
$env:SPRING_AI_ENABLED="true"
$env:SPRING_AI_CHAT_ENABLED="true"
$env:SPRING_AI_MODEL_CHAT="openai"
$env:SPRING_AI_OPENAI_API_KEY="你的 API Key"
$env:SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL="gpt-4.1-mini"
```

With this provider, Spring AI only returns planner text. The Java backend still performs context budgeting, parses the
plan with `AgentPlanParser`, validates it with `AgentPlanValidator`, and executes allowed tools through `ToolRegistry`.
Default tests do not enable Spring AI and do not require any Spring AI environment variables.

Spring AI embedding live smoke is separate from Real Agent validation and does not create tickets, AgentRuns, traces,
vector stores, or database rows:

```powershell
$env:SPRING_AI_ENABLED="true"
$env:SPRING_AI_EMBEDDING_ENABLED="true"
$env:SPRING_AI_MODEL_EMBEDDING="openai"
$env:SPRING_AI_OPENAI_API_KEY="你的 API Key"
mvn test "-Dtest=SpringAiEmbeddingClientLiveSmokeTest" "-Dlive.spring-ai=true" "-Dlive.embedding=true"
```

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
- The selected DashScope model and endpoint do not match. In that case, switch to the endpoint required by DashScope for
  that model or try a compatible model such as `qwen-plus`.

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
