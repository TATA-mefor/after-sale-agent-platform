# AfterSale-Agent Platform

AfterSale-Agent is a Java Spring Boot platform for auditable e-commerce after-sale ticket handling with Agent execution traces.

## Project Overview

V1 proves a narrow enterprise backend loop:

```text
user after-sale message -> ticket -> rule-based AgentRun -> policy retrieval -> low-risk tool call -> trace -> suggestion
```

The project is intentionally built as a modular monolith with Harness Engineering documents, architecture tests, lint
checks, and executable tests as the guardrails.

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

## Docker Compose Local Development

V3.2 adds an optional Docker Compose path for local app + MySQL startup. This is a local development setup only. It is
not a production deployment model and it does not change the default in-memory test path.

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
      "POL-QUALITY-RETURN-EXCHANGE: 质量问题退换货规则"
    ],
    "toolCalls": [
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
      provider: openai
      model: ${AFTERSALE_LLM_MODEL:gpt-4o-mini}
      api-key: ${OPENAI_API_KEY:}
      endpoint: ${OPENAI_RESPONSES_ENDPOINT:https://api.openai.com/v1/responses}
      timeout-seconds: 30
```

If `agent.planner.mode=llm` is selected without `agent.planner.llm.api-key` / `OPENAI_API_KEY`, startup fails with a
clear configuration error. The default `mvn test` path uses `rule` or `fake` and does not require a real LLM, API Key,
or external network.

Current V2.1.1 status: the LLM adapter can call an OpenAI-compatible Responses endpoint when `llm` mode is explicitly
enabled and configuration is complete. The LLM must return structured JSON, which the Java backend parses, validates,
and then executes only through `ToolRegistry`. Tests still use `rule`, `fake`, or a fake `LlmClient`; they never require
a real LLM, API Key, or external network.

### LLM Planner Live Smoke Test

The live smoke test is explicit opt-in. It is not part of the default `mvn test` path.

To run it locally:

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

If `-Dlive.llm=true` is omitted, the test is disabled. If `OPENAI_API_KEY` is missing, the live test is skipped with a
clear message. The smoke test calls only `LlmAgentPlanner` and validates the returned `AgentPlan`; it does not execute
business tools, create `AgentRun`, write `ToolCallTrace`, or mutate tickets.

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
real logistics provider, real payment provider, or real database.

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

Run the focused evaluation tests:

```bash
mvn test -Dtest=EvaluationApplicationServiceTest
```

The default runner uses `RuleBasedAgentPlanner`, validates every generated plan with `AgentPlanValidator`, and computes
intent, subtask, tool, risk, policy-category, approval-requirement, and plan-validity metrics. It does not call a real
LLM provider, require an API key, use LLM-as-judge, mutate tickets or approvals, or connect to external infrastructure.

## V3 Roadmap

V3 is the infrastructure closure phase. V3.1 MySQL Persistence and V3.2 Docker Compose are implemented for explicit
local infrastructure profiles. V3.3 and V3.4 remain planned. V3 does not change the Agent business capability boundary.

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

Planned focus:

- Add structured log fields such as requestId, ticketId, agentRunId, subtaskId, toolName, and approvalRequestId.
- Keep actuator health checks available.
- Do not replace ToolCallTrace or Execution Tree with logs.
- Do not introduce complex Prometheus/Grafana setup unless a later plan requires it.

### V3.4 Final Review

Planned focus:

- Review system capability list.
- Document known limitations.
- Verify demo flow and validation commands.
- Record follow-up directions without claiming unfinished capabilities as completed.

### 真实 LLM 本地运行说明

默认本地运行仍使用 `rule` 模式。若要手动启用真实 LLM Planner，请只在本机环境变量或本地未提交配置中设置：

```text
OPENAI_API_KEY
AFTERSALE_LLM_MODEL
OPENAI_RESPONSES_ENDPOINT
```

`AFTERSALE_LLM_MODEL` 和 `OPENAI_RESPONSES_ENDPOINT` 可选；未设置时分别使用 `gpt-4o-mini` 和 OpenAI Responses
API 默认 endpoint。不要将真实 API Key 写入代码、测试、README、docs、`application.yml` 或提交历史。
