# AfterSale-Agent Platform

AfterSale-Agent is a Java Spring Boot platform for auditable e-commerce after-sale ticket handling with Agent execution traces.

## Project Overview

V1 proves a narrow enterprise backend loop:

```text
user after-sale message -> ticket -> rule-based AgentRun -> policy retrieval -> low-risk tool call -> trace -> suggestion
```

The project is intentionally built as a modular monolith with Harness Engineering documents, architecture tests, lint
checks, and executable tests as the guardrails.

## Core Capabilities

- Create and query after-sale tickets.
- Trigger deterministic AgentRun execution.
- Plan single-intent and multi-intent after-sale tasks.
- Query demo order data through registered tools.
- Retrieve controlled after-sale policy evidence.
- Dispatch specialist handlers for return, exchange, coupon, logistics, general consultation, and human escalation
  subtasks.
- Record ToolCallTrace entries for tool audit.
- Create approval requests for high-risk decisions.
- Query a read-only execution tree for AgentRun inspection.
- Run offline evaluation against a versioned after-sale dataset.
- Run with default in-memory repositories or an explicit MySQL profile.
- Start a local app + MySQL environment with Docker Compose.
- Correlate requests and Agent execution logs with `X-Request-Id` and MDC fields.
- Enrich local MySQL demo data with optional product and order-item seed generated from public datasets.
- Return structured `orderItems` from the `get_order_by_id` order tool for product-level after-sale context.
- Generate item-level return and exchange recommendations from `orderItems` in specialist handlers.
- Discover V4 Skill definitions for return, exchange, coupon, logistics, general consultation, and human escalation
  without changing the existing AgentRun runtime path.

## Tech Stack

- Java 17
- Spring Boot 3.3.x
- Maven
- JUnit 5
- ArchUnit
- Checkstyle
- SpotBugs
- In-memory repositories for default offline demo data
- Spring JDBC + explicit MySQL profile for V3.1 persistence
- Python standard library script for optional V3.5 demo seed generation

## Requirements

- Java 17+
- Maven 3.9+
- Docker and Docker Compose, only for the optional V3.2 local compose flow

## Run Locally

```bash
mvn spring-boot:run
```

Default local startup uses in-memory repositories. It does not require MySQL, Docker, Redis, a real LLM, API keys, or
external network access.

Health checks:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

## MySQL Profile

V3.1 adds an explicit `mysql` profile for local persistence. The default profile remains in-memory, and default
`mvn test` does not connect to MySQL.

The MySQL profile persists:

- Ticket records
- AgentRun records
- ToolCallTrace records
- ApprovalRequest records
- Demo order data
- After-sale policy data

Schema and seed initialization are loaded from:

```text
src/main/resources/schema-mysql.sql
src/main/resources/data-mysql.sql
```

Configure MySQL with local environment variables. Do not commit real passwords.

PowerShell example:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:AFTERSALE_MYSQL_URL = "jdbc:mysql://localhost:3306/after_sale_agent?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
$env:AFTERSALE_MYSQL_USERNAME = "aftersale"
$env:AFTERSALE_MYSQL_PASSWORD = "<local-password>"
mvn spring-boot:run
```

Bash example:

```bash
SPRING_PROFILES_ACTIVE=mysql \
AFTERSALE_MYSQL_URL='jdbc:mysql://localhost:3306/after_sale_agent?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true' \
AFTERSALE_MYSQL_USERNAME=aftersale \
AFTERSALE_MYSQL_PASSWORD='<local-password>' \
mvn spring-boot:run
```

The application only creates a JDBC `DataSource` when `SPRING_PROFILES_ACTIVE=mysql` is set. Without that profile, the
in-memory repositories are active and no database connection is configured.

Manual local verification completed on 2026-05-18:

- Local MySQL version: 8.0.44.
- `schema-mysql.sql` imported successfully.
- `data-mysql.sql` imported successfully.
- `orders` seed count: 6.
- `aftersale_policies` seed count: 6.
- Application startup with the explicit `mysql` profile succeeded.
- Creating a Ticket, triggering an AgentRun, and querying the Execution Tree passed through local HTTP API verification.

This verification used local environment variables only. Do not commit real database passwords, local absolute paths,
API keys, tokens, or production configuration.

## Demo Dataset Enrichment

V3.5 adds optional demo data enrichment for public local datasets. The default app startup and default `mvn test` path
do not require these raw files.

Place downloaded raw datasets under:

```text
data/raw/orders/
data/raw/chinese_reviews/
data/raw/clothing_reviews/
```

Raw CSV, XLSX, JSON, archive, and parquet files under `data/raw` are ignored by Git. Keep only local public downloads
there; do not commit raw large files, personal paths, credentials, or private customer data.

Generate small reviewable seed artifacts:

```bash
python scripts/data/build_demo_seed.py
```

Optional scale controls:

```bash
python scripts/data/build_demo_seed.py \
  --max-orders 1000 \
  --max-products 500 \
  --max-order-items 3000 \
  --max-tickets 500 \
  --max-evaluation-cases 100
```

The script writes:

```text
data/generated/demo_seed_extra.sql
data/generated/demo_evaluation_cases.jsonl
```

Import generated enrichment after the base MySQL schema and seed:

```bash
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < src/main/resources/schema-mysql.sql
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < src/main/resources/data-mysql.sql
mysql --default-character-set=utf8mb4 -u <user> -p after_sale_agent < data/generated/demo_seed_extra.sql
```

The base `data-mysql.sql` already includes minimal `products` and `order_items` rows, so the MySQL demo remains usable
even when the optional generation script is not run.

See `docs/data/DATASET_MAPPING.md` for dataset field mapping, cleaning rules, limits, and current boundaries.

V3.6 wires these product and order-item records into the order query tool output. `get_order_by_id` now returns
structured `orderItems` with product name, category, quantity, price, item status, return/exchange support flags, and
the special-item flag. The default in-memory repository also includes matching demo item data, so this behavior does
not require MySQL or generated raw datasets.

The MySQL `products` and `order_items` tables intentionally store only demo product and line-item fields. The
`supportReturn`, `supportExchange`, and `isSpecialItem` values in Java tool output are deterministic demo-rule
derivations from existing product/category fields; they are not separate MySQL columns.

## Docker Compose Local Development

V3.2 adds an optional Docker Compose path for local app + MySQL startup. This is a local development setup only. It is
not a production deployment model and it does not change the default in-memory test path.
The first run may need to build the local app image and pull base/MySQL images, so startup can be affected by the local
Docker cache and network access.

Start app + MySQL:

```bash
docker compose up --build
```

The compose file starts:

- `mysql` on host port `3306`
- `app` on host port `8080`

The app service runs with:

```text
SPRING_PROFILES_ACTIVE=mysql
AFTERSALE_MYSQL_URL=jdbc:mysql://mysql:3306/after_sale_agent?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
AFTERSALE_MYSQL_USERNAME=aftersale
AFTERSALE_MYSQL_PASSWORD=aftersale
```

These are local placeholder credentials. Override them from your shell or an uncommitted local `.env` file when needed.
Do not commit real passwords, API keys, tokens, or production configuration.

MySQL initialization uses the V3.1 scripts:

```text
src/main/resources/schema-mysql.sql
src/main/resources/data-mysql.sql
```

Check the running app:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

Stop containers:

```bash
docker compose down
```

Stop containers and remove the local MySQL volume:

```bash
docker compose down -v
```

Default validation still does not require Docker:

```bash
mvn test
```

## Observability

V3.3 adds request correlation and structured log fields without introducing an external logging platform. Each HTTP
request returns an `X-Request-Id` response header. If the request already includes `X-Request-Id`, the same value is
returned; otherwise the application generates one.

Example with an explicit request id:

```bash
curl -H "X-Request-Id: demo-request-001" http://localhost:8080/api/health -i
```

Use the returned `X-Request-Id` to search logs for the request. Agent-related logs also include the available business
IDs:

```text
requestId
ticketId
agentRunId
subtaskId
toolName
approvalRequestId
```

Typical troubleshooting flow:

1. Start with `requestId` from the API response header.
2. Find the `ticketId` from ticket creation logs or the API response.
3. Find `agentRunId` from AgentRun logs or the trigger response.
4. Inspect persisted tool audit records:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/traces
```

5. Inspect the read-only execution tree:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/execution-tree
```

Approval flows remain queryable through:

```bash
curl http://localhost:8080/api/approval-requests/pending
curl http://localhost:8080/api/approval-requests/{approvalRequestId}
```

Logs are diagnostic only. ToolCallTrace, ApprovalRequest records, and the Execution Tree API remain the audit and
inspection surfaces. Logs must not contain API keys, database passwords, full LLM prompts, sensitive credentials, or
long raw user text.

## Core API List

Health:

```bash
GET /api/health
GET /actuator/health
```

Tickets:

```bash
POST /api/tickets
GET /api/tickets/{ticketId}
```

Agent execution:

```bash
POST /api/tickets/{ticketId}/agent-runs
GET /api/agent-runs/{runId}/traces
GET /api/agent-runs/{runId}/execution-tree
```

Approval:

```bash
GET /api/approval-requests/pending
GET /api/approval-requests/{approvalRequestId}
POST /api/approval-requests/{approvalRequestId}/approve
POST /api/approval-requests/{approvalRequestId}/reject
```

The execution tree endpoint is read-only. It aggregates AgentRun, subtasks, ToolCallTrace records, ApprovalRequest
records, and workspace-derived summaries without mutating business state.

## Demo Walkthrough

This walkthrough shows the V1 closed loop:

```text
create ticket -> trigger AgentRun -> query ticket -> query tool traces
```

Start the application:

```bash
mvn spring-boot:run
```

Check that the application is up:

```bash
curl http://localhost:8080/api/health
```

Expected result:

```json
{
  "status": "UP",
  "service": "after-sale-agent-platform"
}
```

Create an after-sale ticket:

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"userId":"U-1001","orderId":"O202605130001","message":"我买的耳机有质量问题，左耳没声音，想退货退款。"}'
```

Expected result:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": "T-...",
    "status": "CREATED",
    "intentType": "UNKNOWN"
  }
}
```

Copy `data.ticketId`, then trigger the Agent:

```bash
curl -X POST http://localhost:8080/api/tickets/{ticketId}/agent-runs
```

Expected result:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "runId": "RUN-...",
    "status": "SUCCEEDED",
    "intent": "RETURN_AND_REFUND",
    "plan": "{...}",
    "finalSuggestion": "Intent RETURN_AND_REFUND identified...",
    "evidence": [
      "Order O202605130001: Wireless Headphones...",
      "POL-QUALITY-RETURN-EXCHANGE: 质量问题退换货规则"
    ],
    "toolCalls": [
      "get_order_by_id",
      "search_aftersale_policy",
      "add_ticket_note"
    ]
  }
}
```

Query the ticket again:

```bash
curl http://localhost:8080/api/tickets/{ticketId}
```

Expected result:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "ticketId": "T-...",
    "intentType": "RETURN_AND_REFUND",
    "status": "RESOLVED",
    "internalNote": "Intent RETURN_AND_REFUND identified...",
    "agentSuggestion": "Intent RETURN_AND_REFUND identified..."
  }
}
```

Copy `data.runId` from the AgentRun response, then query trace:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/traces
```

Expected result:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": [
    {
      "runId": "RUN-...",
      "toolName": "get_order_by_id",
      "status": "SUCCEEDED",
      "inputJson": "{\"orderId\":\"O202605130001\"}",
      "outputJson": "{\"orderId\":\"O202605130001\",\"orderItems\":[{\"productName\":\"Wireless Headphones\",\"supportReturn\":true}]}"
    },
    {
      "runId": "RUN-...",
      "toolName": "search_aftersale_policy",
      "status": "SUCCEEDED",
      "inputJson": "{\"query\":\"...\"}",
      "outputJson": "{\"results\":[...]}"
    },
    {
      "runId": "RUN-...",
      "toolName": "add_ticket_note",
      "status": "SUCCEEDED",
      "inputJson": "{\"ticketId\":\"T-...\",\"note\":\"...\"}",
      "outputJson": "{\"ticketId\":\"T-...\",...}"
    }
  ]
}
```

API responses use a shared envelope:

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {}
}
```

For a richer V2/V3 demo, create a multi-intent or high-risk ticket, trigger an AgentRun, then inspect both the approval
queue and execution tree:

```bash
curl http://localhost:8080/api/approval-requests/pending
curl http://localhost:8080/api/agent-runs/{runId}/execution-tree
```

## Validate

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## V1 Capability Boundary

V1 includes:

- Ticket creation and ticket detail query APIs.
- Rule-based intent classification for the demo after-sale messages.
- In-memory after-sale policy retrieval.
- Tool boundary through `ToolRegistry`.
- Low-risk tool execution for `search_aftersale_policy` and `add_ticket_note`.
- `AgentRun` and `ToolCallTrace` records exposed through APIs.
- Architecture checks that prevent API/repository coupling, domain/Spring Web coupling, direct Agent repository access,
  direct tool repository access, and business-module dependency on Agent orchestration.

## V1 Does Not Do

- No real LLM provider.
- No real database, Redis, payment, refund, inventory, or logistics integration.
- No real order lookup tool in the final V1 implementation; the demo carries `orderId` on the ticket and focuses on
  policy evidence plus auditable tool execution.
- No complex frontend or production authentication.
- No multi-Agent orchestration.
- No automatic execution of high-risk actions.

Start with these project documents:

- `SPEC.md`
- `WORKFLOW.md`
- `AGENTS.md`
- `ARCHITECTURE.md`
- `EXEC_PLAN_V1.md`

## V2 Roadmap

V1 当前是规则型 AgentRun 闭环，已经支持：

```text
创建售后工单
触发 AgentRun
查询内存 demo 订单
检索售后政策
写入工单备注
记录 ToolCallTrace
查询执行轨迹
```

V2 的第一优先级是接入真实 LLM Planner Adapter。

### V2.1 LLM Planner Adapter

目标：

```text
用户售后问题
→ Ticket
→ LLM Planner 生成结构化 AgentPlan
→ Java 后端校验 AgentPlan
→ ToolRegistry 执行工具
→ ToolCallTrace 记录轨迹
```

设计边界：

- LLM 只负责规划；
- Java 后端负责执行；
- ToolRegistry 仍然是唯一工具执行入口；
- 高风险动作仍然需要审批；
- 默认测试不依赖真实 LLM；
- API Key 只能来自环境变量或本地配置。

### Planner Mode Configuration

The application selects the Agent planner with:

```yaml
agent:
  planner:
    mode: rule
```

Supported modes:

- `rule`: default mode. Uses `RuleBasedAgentPlanner`, preserves V1 behavior, and requires no API Key.
- `fake`: deterministic test mode. Uses `FakeAgentPlanner` and does not call any external network.
- `llm`: LLM adapter mode. Requires an API Key from environment or local configuration.

Example local LLM configuration:

```yaml
agent:
  planner:
    mode: llm
    llm:
      provider: ${AFTERSALE_LLM_PROVIDER:openai-responses}
      model: ${AFTERSALE_LLM_MODEL:gpt-4.1-mini}
      api-key: ${OPENAI_API_KEY:}
      endpoint: ${OPENAI_RESPONSES_ENDPOINT:https://api.openai.com/v1/responses}
      timeout-seconds: 30
      dashscope:
        api-key: ${DASHSCOPE_API_KEY:}
        base-url: ${DASHSCOPE_BASE_URL:https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1}
        responses-endpoint: ${DASHSCOPE_RESPONSES_ENDPOINT:}
        chat-completions-endpoint: ${DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT:}
      budget:
        system-prompt-tokens: 2000
        history-tokens: 4000
        rag-context-tokens: 8000
        tool-catalog-tokens: 2000
        max-output-tokens: 1000
        total-input-tokens: 16000
```

Supported live providers:

- `openai-responses`: OpenAI Responses API. Uses `OPENAI_API_KEY` and `OPENAI_RESPONSES_ENDPOINT`.
- `dashscope-responses`: DashScope Responses-compatible endpoint. Uses `DASHSCOPE_API_KEY` and either
  `DASHSCOPE_RESPONSES_ENDPOINT` or `${DASHSCOPE_BASE_URL}/responses`.
- `dashscope-chat-compatible`: DashScope OpenAI-compatible Chat Completions endpoint. Uses `DASHSCOPE_API_KEY` and
  either `DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT` or `${DASHSCOPE_BASE_URL}/chat/completions`.

If `agent.planner.mode=llm` is selected without the provider-specific API Key, startup fails with a clear configuration
error. The default `mvn test` path uses `rule` or `fake` and does not require a real LLM, API Key, or external network.

Current V2.1.1 status: the LLM adapter can call an OpenAI-compatible Responses endpoint when `llm` mode is explicitly
enabled and configuration is complete. The LLM must return structured JSON, which the Java backend parses, validates,
and then executes only through `ToolRegistry`. Tests still use `rule`, `fake`, or a fake `LlmClient`; they never require
a real LLM, API Key, or external network.

V3.10 adds DashScope / Qwen provider adapters without changing the planner boundary. The LLM still only returns an
`AgentPlan`; Java still validates it and executes tools through `ToolRegistry`.

DashScope Chat Completions compatible example for PowerShell:

```powershell
$env:AFTERSALE_LLM_PROVIDER="dashscope-chat-compatible"
$env:DASHSCOPE_API_KEY="你的 DashScope API Key"
$env:DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1"
$env:AFTERSALE_LLM_MODEL="qwen3.6-plus"
```

DashScope Responses compatible example for PowerShell:

```powershell
$env:AFTERSALE_LLM_PROVIDER="dashscope-responses"
$env:DASHSCOPE_API_KEY="你的 DashScope API Key"
$env:DASHSCOPE_BASE_URL="https://dashscope.aliyuncs.com/api/v2/apps/protocols/compatible-mode/v1"
$env:AFTERSALE_LLM_MODEL="qwen3.6-plus"
```

If a provider reports that a model does not match the selected endpoint, switch to the endpoint required by DashScope
for that model or try a compatible model such as `qwen-plus`. Provider errors are summarized with provider, endpoint
host, model, status code, and a sanitized response body; API keys and full prompts are not logged.

### Context Budget / Token Observability

V3.8 adds a deterministic prompt budget layer before `LlmAgentPlanner` calls an LLM provider. The planner prompt is
assembled from typed sections instead of one uncontrolled string.

Critical sections are never silently dropped:

- `systemInstructions`
- `outputSchema`
- `plannerContractSummary`
- `toolCatalogCompact`
- `riskPolicySummary`
- `ticketContext`

Optional sections can be reduced by budget policy:

- `conversationHistory`
- `ragContext`
- `examples`
- `debugHints`
- `extendedPolicyText`
- `nonEssentialDocs`

Overflow handling drops or compresses optional content first: debug hints, examples, conversation history, RAG context,
extended policy text, then non-critical ticket context fields. If the prompt still exceeds the configured input budget,
the planner returns a clear prompt-budget error instead of truncating the output schema, compact tool catalog, or risk
policy summary.

The tool catalog sent to the LLM is compact and planner-safe. It includes only tool name, risk level, required input
fields, and short purpose. Full tool contracts are not copied into the prompt.

Before each LLM request, the planner logs only estimated token telemetry and budget actions:

```text
systemPromptTokens
plannerContractTokens
toolCatalogTokens
ticketContextTokens
orderContextTokens
historyTokens
ragContextTokens
optionalTokensDropped
totalInputTokens
maxOutputTokens
budgetExceeded
budgetAction
```

The estimate is intentionally simple: `max(1, chars / 4)`. Logs do not contain full prompt text, API keys, database
passwords, credentials, or long raw documents. Provider output token usage is recorded as `unknown` unless the provider
client exposes a safe usage field.

### LLM Planner Live Smoke Test

The live smoke test is explicit opt-in. It is not part of the default `mvn test` path.

To run it locally:

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
```

Required local environment:

```text
OPENAI_API_KEY for openai-responses, or DASHSCOPE_API_KEY for dashscope providers
```

Optional local environment:

```text
AFTERSALE_LLM_PROVIDER
AFTERSALE_LLM_MODEL
OPENAI_RESPONSES_ENDPOINT
DASHSCOPE_BASE_URL
DASHSCOPE_RESPONSES_ENDPOINT
DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT
```

If `-Dlive.llm=true` is omitted, the test is disabled. If the selected provider API key is missing, the live test is
skipped with a clear message. The smoke test calls only `LlmAgentPlanner` and validates the returned `AgentPlan`; it
does not execute business tools, create `AgentRun`, write `ToolCallTrace`, or mutate tickets.

### Real LLM + MySQL Seed Data Opt-In Validation

V3.9 adds a separate live validation path for the full HTTP AgentRun chain. It is disabled by default and runs only when
both live flags and all required environment variables are present.

Run command:

```bash
mvn test -Dtest=RealAgentValidationLiveTest -Dlive.llm=true -Dlive.mysql=true
```

Required environment:

```text
OPENAI_API_KEY for openai-responses, or DASHSCOPE_API_KEY for dashscope providers
AFTERSALE_MYSQL_URL
AFTERSALE_MYSQL_USERNAME
AFTERSALE_MYSQL_PASSWORD
```

Optional environment:

```text
AFTERSALE_LLM_PROVIDER
OPENAI_RESPONSES_ENDPOINT
DASHSCOPE_BASE_URL
DASHSCOPE_RESPONSES_ENDPOINT
DASHSCOPE_CHAT_COMPLETIONS_ENDPOINT
AFTERSALE_LLM_MODEL
AFTERSALE_LIVE_ORDER_ID
```

If `AFTERSALE_LIVE_ORDER_ID` is not set, the test uses the documented seed order `O202605130001`. The test starts the
Spring application with the `mysql` profile and `agent.planner.mode=llm`, then drives the same HTTP APIs used by a
manual demo:

```text
POST /api/tickets
POST /api/tickets/{ticketId}/agent-runs
GET /api/agent-runs/{runId}/execution-tree
GET /api/agent-runs/{runId}/traces
```

The live validation checks that the real LLM planner produces a valid `AgentPlan`, the backend validates it, tools still
execute through `ToolRegistry`, MySQL seed order facts include `orderItems`, `ToolCallTrace` rows are written, and the
execution tree exposes item-level recommendation evidence. Missing flags or environment variables skip the test before
loading the MySQL/LLM application context. Provider 403, `INSUFFICIENT_BALANCE`, or insufficient-balance responses are
reported as provider/account-balance setup errors for the explicit live run. See
[docs/demo/REAL_AGENT_VALIDATION.md](docs/demo/REAL_AGENT_VALIDATION.md) for the full setup checklist.

### V2 后续方向

- MySQL Persistence；
- Vector or Hybrid Policy Retrieval；
- Docker Compose and Observability。

### V2.2 Order Query Tools

V2.2 adds two low-risk order tools backed by in-memory demo data:

- `get_order_by_id`
- `get_user_orders`

The rule-based AgentRun now plans `get_order_by_id` before policy retrieval, so the final suggestion and trace can show
both order facts and policy evidence. This is still demo data only; the project does not connect to a real order center,
real logistics provider, real payment provider, or production order database.

V3.6 enriches `get_order_by_id` with structured `orderItems`. Each item includes:

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

`get_user_orders` may include the same structured item list because it uses the shared order tool output mapper, but it
remains intended as a lightweight user-order lookup. ToolCallTrace and Execution Tree output can inspect the serialized
`orderItems` through the `get_order_by_id` `outputJson`.

V3.7 uses the same `orderItems` evidence inside Return and Exchange specialist handlers. The handlers still obtain order
data only through `ToolRegistry`, then write item-level recommendations into the final summary and Ticket note. A
recommendation includes the selected `orderItemId`, `productId`, `productName`, `category`, support flags, special-item
flag, recommendation text, and reason.

Current deterministic matching rules are intentionally simple:

- Prefer an item whose `productName` appears in the subtask target or user message.
- Otherwise match by `category`.
- Treat common clothing words such as `裙子`, `衣服`, `上衣`, `裤子`, `服装`, and `尺码` as coarse clothing signals.
- If no item matches, fall back to the first item and state that fallback in the reason.

Unsupported or special items do not produce a direct return/exchange recommendation. The handler recommends policy or
manual-review handling instead and does not execute real refund, exchange, inventory, logistics, or payment actions.

### V2.3 Multi-Intent Planning

V2.3 adds Multi-Intent Planning while preserving the V2.2 single-intent flow.

Implemented flow:

```text
complex after-sale message
→ RuleBasedAgentPlanner / LLM AgentPlan with subtasks
→ structured AgentSubtask list
→ Java validation
→ sequential ToolRegistry execution
→ ToolCallTrace with subtask metadata
→ summarized final suggestion
```

Example user message:

```text
我买了三件衣服，其中一件有污渍要退货，另一件要换尺码，还有一张优惠券没用上怎么退？
```

Expected subtask types:

- `RETURN`
- `EXCHANGE`
- `COUPON_CONSULTATION`

The rule-based planner detects this pattern and creates multiple subtasks. Each subtask uses ToolRegistry tools in
priority order, and trace `inputJson` includes subtask metadata such as `subtaskId` and `subtaskType`.

V2.3 remains a single-process planning and execution design. It does not add multi-Agent microservices, queues, parallel
execution, voting consensus, a full coupon system, real refunds, real exchanges, real logistics, or real payment
integration. V2.4 adds handler-based dispatch on top of this subtask model, and V2.8 exposes the resulting execution
tree as a read-only API.

### V2.4 Specialist Agent Handler

V2.4 adds Specialist Agent Handler dispatch for multi-intent subtasks while preserving the V2.2 single-intent flow and
the V2.3 sequential execution model.

Implemented flow:

```text
AgentPlan with subtasks
→ AgentApplicationService
→ SpecialistAgentHandlerRegistry
→ SpecialistAgentHandler
→ ToolRegistry
→ ToolCallTrace
→ SubtaskExecutionResult
→ final summary
```

Implemented handlers:

- `ReturnAgentHandler`
- `ExchangeAgentHandler`
- `CouponAgentHandler`
- `LogisticsAgentHandler`
- `GeneralConsultationHandler`
- `HumanEscalationHandler`

Each handler declares the `SubtaskType` it supports, and `SpecialistAgentHandlerRegistry` rejects duplicate coverage.
Unsupported subtask types return a structured failed `SubtaskExecutionResult`. Handlers call only registered tools
through `ToolRegistry`, so `ToolCallTrace` continues to record handler-triggered calls.

V2.4 remains a modular-monolith strategy-class design. It does not add multi-Agent microservices, queues, parallel
execution, voting consensus, real refunds, real exchanges, real coupon compensation, real logistics, real payment
integration, or a real LLM dependency in default tests.

### V2.5 Policy Retrieval Tool

V2.5 completes controlled policy retrieval for handler execution. The public tool remains:

```text
search_aftersale_policy
```

Implemented boundary:

```text
SpecialistAgentHandler
→ ToolRegistry
→ SearchAfterSalePolicyToolExecutor
→ PolicyApplicationService
→ PolicyRepository
→ InMemoryPolicyRepository
```

The retrieval result is structured as policy snippets and empty matches return `results: []` with a clear message.
V2.5 does not add VectorStore, PGvector, embeddings, network search, real LLM dependency, real refunds, real exchanges,
coupon compensation, payment changes, logistics changes, or a real database.

### V2.6 Agent Workspace / Structured Memory

V2.6 adds single-`AgentRun` structured workspace memory.

Implemented flow:

```text
AgentRun creates AgentWorkspace
→ SpecialistAgentHandler reads workspace context
→ Handler executes tools through ToolRegistry
→ Handler writes order facts, policy evidence, tool summaries, subtask memory, and risk flags
→ final summary is assembled from workspace
```

Candidate models:

- `AgentWorkspace`
- `OrderFact`
- `PolicyEvidence`
- `SubtaskMemory`
- `ToolResultSummary`
- `RiskFlag`

V2.6 is scoped to one `AgentRun`. It is not long-term memory, user profiling, vector memory, cross-session memory,
Redis, MySQL, or a vector database. Workspace must not store API keys, sensitive credentials, full long prompts, or raw
long LLM outputs, and it must not replace `ToolCallTrace` or bypass `ToolRegistry`.

### V2.7 Approval APIs

V2.7 adds in-memory approval APIs for high-risk Agent decisions. Approval records are structured and queryable, but
approval does not execute real refunds, exchanges, coupon compensation, payment changes, or logistics changes.

Implemented APIs:

```bash
curl http://localhost:8080/api/approval-requests/pending

curl http://localhost:8080/api/approval-requests/{approvalRequestId}

curl -X POST http://localhost:8080/api/approval-requests/{approvalRequestId}/approve \
  -H "Content-Type: application/json" \
  -d '{"reviewerId":"operator-1","reason":"Evidence is sufficient for manual processing."}'

curl -X POST http://localhost:8080/api/approval-requests/{approvalRequestId}/reject \
  -H "Content-Type: application/json" \
  -d '{"reviewerId":"operator-1","reason":"Policy evidence is insufficient."}'
```

High-risk subtasks create `ApprovalRequest` records and move the ticket to `WAITING_HUMAN_APPROVAL`. Low-risk tools do
not create approval requests.

### V2.8 Execution Tree

V2.8 adds a read-only execution tree API for inspecting one `AgentRun` across plan, subtasks, tool traces, approval
requests, and workspace-derived summary.

Implemented API:

```bash
curl http://localhost:8080/api/agent-runs/{runId}/execution-tree
```

The response contains root AgentRun fields, root-level tool calls without `subtaskId`, subtask nodes with attached tool
calls, approval request nodes, errors, and timestamps. Tool calls are associated to subtasks through
`ToolCallTrace.inputJson.subtaskId` when present. Approval requests are associated through `runId` and `subtaskId` when
available.

This API is query-only. It does not modify tickets, AgentRun records, traces, approvals, refunds, exchanges, coupons,
payments, logistics, or inventory.

### V2.9 Evaluation Dataset

V2.9 adds an offline evaluation dataset and runner for the current after-sale Agent planner.

Dataset:

```text
docs/evaluation/aftersale_cases.jsonl
```

Evaluation guide:

```text
docs/evaluation/EVALUATION.md
```

Run the focused evaluation tests:

```bash
mvn test -Dtest=EvaluationApplicationServiceTest
```

The default runner uses `RuleBasedAgentPlanner`, validates every generated plan with `AgentPlanValidator`, and computes
intent, subtask, tool, risk, policy-category, approval-requirement, and plan-validity metrics. It does not call a real
LLM provider, require an API key, use LLM-as-judge, mutate tickets or approvals, or connect to external infrastructure.

## V3 Roadmap

V3 is the infrastructure closure phase. V3.1 MySQL Persistence and V3.2 Docker Compose are implemented for explicit
local infrastructure profiles. V3.3 Structured Logging / Observability is implemented for request correlation and
structured diagnostic fields. V3.4 Final Review is completed as the infrastructure closure review. V3.5 Demo Dataset
Enrichment is implemented for optional local seed generation. V3.6 Order Items Tool Enrichment is implemented so
existing order tools can expose product-level order detail. V3.7 Item-Specific Recommendation is implemented so Return
and Exchange specialist handlers can produce item-level suggestions from those tool results. V3.8 Context Budget /
Token Observability is implemented so real LLM planning can be introduced behind explicit budget and telemetry
controls. V3 does not change the Agent business capability boundary.

### V3.1 MySQL Persistence

Implemented focus:

- Persist Ticket, AgentRun, ToolCallTrace, and ApprovalRequest records.
- Seed order demo data and policy data.
- Add a local `mysql` profile.
- Keep in-memory/test profile for offline deterministic tests.
- Keep default `mvn test` independent from MySQL, Docker, Redis, real LLMs, API keys, and external network.
- Keep database credentials in environment variables or uncommitted local configuration only.

### V3.2 Docker Compose

Implemented focus:

- Add local app + mysql startup through Docker Compose.
- Build the app from the local Dockerfile.
- Initialize MySQL from `schema-mysql.sql` and `data-mysql.sql`.
- Run the app with the explicit `mysql` profile.
- Keep Redis out of the default compose environment.
- Manage local credentials through placeholder defaults and environment variable overrides.
- Do not commit real secrets.
- Treat Docker Compose as local development setup, not production deployment.

### V3.3 Structured Logging / Observability

Implemented focus:

- Add `X-Request-Id` request propagation and response header return.
- Add MDC-backed structured log fields such as requestId, ticketId, agentRunId, subtaskId, toolName, and
  approvalRequestId.
- Log key Ticket, AgentRun, Specialist Handler, ToolRegistry, Approval, and Execution Tree paths.
- Keep actuator health checks available.
- Do not replace ToolCallTrace or Execution Tree with logs.
- Do not introduce Prometheus/Grafana, ELK, OpenTelemetry, or external logging platforms.

### V3.4 Final Review

Completed focus:

- Review system capability list.
- Document known limitations.
- Verify demo flow and validation commands.
- Record follow-up directions without claiming unfinished capabilities as completed.

### V3.5 Demo Dataset Enrichment

Implemented focus:

- Add `products` and `order_items` MySQL tables for richer local demo data.
- Keep minimal product and order-item seed in `data-mysql.sql`.
- Keep raw downloaded datasets out of Git under `data/raw`.
- Generate optional enrichment SQL and JSONL cases under `data/generated`.
- Document mapping from public order, Chinese review, and clothing feedback datasets.
- Keep default startup and default tests independent from external dataset files.

### V3.6 Order Items Tool Enrichment

Implemented focus:

- Extend the order domain and tool output with structured `orderItems`.
- Query `order_items` joined with `products` in the explicit MySQL profile.
- Keep default in-memory order demo data with at least one item per seeded order.
- Preserve Agent, Handler, ToolRegistry, Approval, Trace, and Workspace execution boundaries.
- Keep default tests independent from MySQL, Docker, raw datasets, real LLMs, API keys, and external network.

### V3.7 Item-Specific Recommendation

Implemented focus:

- Parse `get_order_by_id` `orderItems` into AgentWorkspace order facts.
- Generate item-level return recommendations in `ReturnAgentHandler`.
- Generate item-level exchange recommendations in `ExchangeAgentHandler`.
- Match items deterministically by product name, category, or coarse clothing keywords.
- Fall back to the first order item with an explicit reason when no specific item can be matched.
- Respect Java-derived `supportReturn`, `supportExchange`, and `isSpecialItem` flags.
- Keep Handler access to order data behind ToolRegistry and avoid real refund/exchange/inventory actions.

### V3.8 Context Budget / Token Observability

Implemented focus:

- Split LLM planner prompts into typed critical and optional sections.
- Apply a deterministic input-token budget using a simple `max(1, chars / 4)` estimate.
- Keep critical schema, compact tool catalog, risk policy summary, and ticket context from being silently dropped.
- Reduce optional context in a fixed order before returning a clear budget error.
- Send only a compact tool catalog to the planner prompt.
- Log estimated token telemetry and budget actions without logging full prompt text or secrets.
- Keep default tests independent from real LLMs, MySQL, Docker, API keys, and external network.

### V3.9 Real LLM + MySQL Seed Data Opt-In Validation

Implemented focus:

- Add `RealAgentValidationLiveTest` as an explicit live-only HTTP validation path.
- Require both `-Dlive.llm=true` and `-Dlive.mysql=true` before the test can run.
- Require LLM and MySQL environment variables before the Spring `mysql` profile context is loaded.
- Exercise Ticket creation, AgentRun creation, LLM planning, ToolRegistry execution, traces, and execution-tree query
  through HTTP APIs.
- Use the MySQL seed order `O202605130001` by default, with `AFTERSALE_LIVE_ORDER_ID` as a local override.
- Keep default tests independent from real LLMs, MySQL, Docker, API keys, and external network.

## Known Limitations

- The default runtime uses in-memory repositories, so default local data is reset on restart.
- MySQL persistence is available only through the explicit `mysql` profile.
- Docker Compose is a local development setup, not a production deployment.
- The default Agent planner is deterministic rule-based fallback; real LLM mode is explicit opt-in.
- The live LLM smoke test is manual opt-in and requires local credentials.
- No production authentication or authorization is implemented.
- No real refund, exchange, coupon compensation, payment, inventory, logistics, order center, or dispute-closing system
  is connected.
- Approval APIs record manual decisions but do not execute real high-risk business actions.
- Policy retrieval is controlled local keyword retrieval, not vector search or hybrid retrieval.
- Logs are diagnostic only; ToolCallTrace, ApprovalRequest records, and Execution Tree remain the audit surfaces.
- Demo dataset enrichment is optional; V3.6 exposes available `products` and `order_items` data through order tool
  output, but it remains demo data and does not connect to a production order center.
- V3.7 item-level recommendations are deterministic demo guidance. Support flags are derived in Java from existing
  product/category fields, not read from dedicated MySQL columns.
- V3.8 token counts are estimates, not provider tokenizer counts. Provider output/cache usage remains `unknown` unless
  a future provider client safely exposes usage metadata.
- V3.9 live validation is opt-in and may fail for local setup reasons such as provider balance, MySQL availability, seed
  import state, or non-deterministic provider output. It is intentionally outside default validation.
- Docker, MySQL, Redis, real LLMs, API keys, and external network access are intentionally outside the default
  `mvn test` path.

### 真实 LLM 本地运行说明

默认本地运行仍使用 `rule` 模式。若要手动启用真实 LLM Planner，请只在本机环境变量或本地未提交配置中设置：

```text
OPENAI_API_KEY
AFTERSALE_LLM_MODEL
OPENAI_RESPONSES_ENDPOINT
```

`AFTERSALE_LLM_MODEL` 和 `OPENAI_RESPONSES_ENDPOINT` 可选；未设置时分别使用 `gpt-4o-mini` 和 OpenAI Responses
API 默认 endpoint。不要将真实 API Key 写入代码、测试、README、docs、`application.yml` 或提交历史。

## V4 Roadmap: RAG, Spring AI, Tool / Skill Layer

V4 focuses on interview-critical AI engineering capabilities:

- Spring AI provider adapter;
- RAG / vectorized after-sale policy retrieval;
- PostgreSQL + PGvector opt-in profile;
- Policy document ingestion, chunking, embedding, and evidence retrieval;
- Tool / Skill capability layer;
- Execution Tree evidence visualization;
- Spring Boot completeness improvements.

V4.0 pre-flight fixes, V4.1 Tool / Skill Layer Foundation, and V4.2 Spring AI Adapter are completed. Skill is now a
first-class Java contract and registry concept, while the current AgentRun execution path still uses the existing
Specialist Handler dispatch. Spring AI is available as an optional provider adapter and is disabled by default.

V4 preserves the existing Agent safety model:

```text
LLM plans only.
Skill orchestrates safely.
ToolRegistry executes atomic tools.
RAG retrieves policy evidence.
ToolCallTrace records tool calls.
Approval blocks high-risk actions.
```

### Planned V4 Profiles

```text
default       -> in-memory / fake embedding / no external dependency
mysql         -> existing V3 MySQL persistence
rag-postgres  -> PostgreSQL + PGvector for policy RAG
spring-ai-live -> explicit live provider validation
```

### Planned V4 Demo Flow

```text
Policy document ingestion
→ chunking
→ embedding
→ vector store write
→ ticket creation
→ AgentRun
→ SkillRegistry
→ search_aftersale_policy with HYBRID retrieval
→ ToolCallTrace
→ AgentWorkspace.PolicyEvidence
→ Execution Tree evidence node
→ final suggestion with policy evidence
```

### V4.1 Tool / Skill Foundation

Implemented V4.1 foundation:

- `AgentSkill`, `SkillDefinition`, `SkillRegistry`, `SkillExecutionContext`, and `SkillExecutionResult`;
- `SpecialistHandlerSkillAdapter` for wrapping existing Specialist Handlers without changing their ToolRegistry path;
- Skill definitions for return eligibility, exchange recommendation, coupon consultation, logistics analysis, general
  consultation, and human approval routing;
- Skill risk validation so a Skill cannot claim a lower risk than its required tools;
- Architecture checks that prevent Skill code from depending directly on repositories, Spring Web, LLM infrastructure,
  Spring AI, vector/RAG infrastructure, or concrete tool executors.

V4.1 does not implement Spring AI, RAG, PGvector, policy ingestion, Execution Tree skill nodes, or full runtime migration
from `SpecialistAgentHandlerRegistry` to `SkillRegistry`. `plannedSkills` remains a documented future extension and is
not generated, parsed, or executed by default.

### V4.2 Spring AI Adapter

Implemented V4.2 adapter foundation:

- `spring-ai-chat` LLM provider routes through `SpringAiLlmClient`, then returns plain text to the existing
  `LlmAgentPlanner`;
- provider output still passes through `AgentPlanParser` and `AgentPlanValidator`;
- Spring AI `ChatClient` is kept inside `agent.infrastructure.springai` and is not exposed to Agent, Handler, Skill,
  ToolRegistry, Repository, or domain code;
- `EmbeddingClient`, `FakeEmbeddingClient`, and `SpringAiEmbeddingClient` establish the embedding provider boundary
  for later RAG work;
- default configuration disables Spring AI model auto-creation with `spring.ai.model.*=none` unless explicitly enabled;
- live Spring AI smoke tests are opt-in and do not create tickets, AgentRuns, traces, vector stores, or database rows.

V4.2 does not implement RAG, VectorStore, PGvector, policy ingestion, Spring AI tool/function calling, or any direct
tool execution by the provider.

Spring AI chat live example:

```powershell
$env:AFTERSALE_LLM_PROVIDER="spring-ai-chat"
$env:SPRING_AI_ENABLED="true"
$env:SPRING_AI_CHAT_ENABLED="true"
$env:SPRING_AI_MODEL_CHAT="openai"
$env:SPRING_AI_OPENAI_API_KEY="你的 API Key"
$env:SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL="gpt-4.1-mini"
mvn test "-Dtest=SpringAiLlmClientLiveSmokeTest" "-Dlive.spring-ai=true" "-Dlive.llm=true"
```

Spring AI embedding live example:

```powershell
$env:SPRING_AI_ENABLED="true"
$env:SPRING_AI_EMBEDDING_ENABLED="true"
$env:SPRING_AI_MODEL_EMBEDDING="openai"
$env:SPRING_AI_OPENAI_API_KEY="你的 API Key"
mvn test "-Dtest=SpringAiEmbeddingClientLiveSmokeTest" "-Dlive.spring-ai=true" "-Dlive.embedding=true"
```

Spring AI provider only supplies planner text or embedding vectors through project-owned adapters. Project tools are
still executed only by Java through `ToolRegistry`; do not register `ToolRegistry` tools as Spring AI tool callbacks.

### V4 Default Test Boundary

Default validation remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Default validation must not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, or external network.

### V4 Non-goals

V4 does not implement real refund, real exchange, real payment, real logistics, real inventory mutation, real coupon compensation, production authentication, microservices, or a LangChain sidecar main path.
