# AfterSale-Agent Platform

[![CI](https://github.com/TATAme/after-sale-agent-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/TATAme/after-sale-agent-platform/actions/workflows/ci.yml)
[![Java 17](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

AfterSale-Agent is a Java Spring Boot platform for auditable e-commerce after-sale ticket handling with Agent execution traces.

## Project Overview

V1 proves a narrow enterprise backend loop:

```text
user after-sale message -> ticket -> rule-based AgentRun -> policy retrieval -> low-risk tool call -> trace -> suggestion
```

The project is intentionally built as a modular monolith with Harness Engineering documents, architecture tests, lint
checks, and executable tests as the guardrails.

V4 status: completed. The final V4 scope delivered enterprise-grade Agent platform foundation work, RAG policy
evidence, Spring AI / PGvector boundaries, Actuator health, OpenAPI docs, interview demo docs, and default offline
validation. This does not mean production external integrations are complete. Real refund, exchange, payment,
logistics, production auth, production monitoring, and production deployment remain future work.

- [V4 Final Completion Record](version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md)
- [V4 Release Summary](version-updates/V4_RELEASE_SUMMARY.md)
- [中文项目整改方案](docs/quality/PROJECT_REMEDIATION_PLAN.md)
- [Project Review Correction Stage 0](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE0.md)
- [Production Config Template](docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md)
- [Project Review Correction Stage 1](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE1_PROD_CONFIG_TEMPLATE.md)
- [Observability Hardening Decision](docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md)
- [Project Review Correction Stage 2](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE2_OBSERVABILITY_HARDENING.md)
- [API Completeness Decision](docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md)
- [Project Review Correction Stage 3.1](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_1_API_COMPLETENESS_DECISION.md)
- [Project Review Correction Stage 3.2](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_2_TICKET_PAGINATION.md)
- [Project Review Correction Stage 3.3](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_3_AGENT_RUN_STATUS_READ.md)
- [Async / Streaming / Batch API Decision](docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md)
- [Project Review Correction Stage 3.4](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_4_ASYNC_STREAMING_BATCH_EVALUATION.md)
- [Spring AI Deepening Decision](docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md)
- [Project Review Correction Stage 4](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE4_SPRING_AI_DEEPENING_EVALUATION.md)
- [RAG Quality Improvement Decision](docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md)
- [Project Review Correction Stage 5](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE5_RAG_QUALITY_EVALUATION.md)
- [Deployment Hardening Decision](docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md)
- [Deployment Hardening Roadmap](docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md)
- [Project Review Correction Stage 6](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE6_DEPLOYMENT_HARDENING_ROADMAP.md)
- [V5.A.1 JdbcPolicyVectorRepository](version-updates/EXEC_PLAN_V5_A1_JDBC_POLICY_VECTOR_REPOSITORY.md)
- [V5.A.2 Schema Init / Version Baseline](version-updates/EXEC_PLAN_V5_A2_SCHEMA_INIT_VERSION_BASELINE.md)
- [V5.A.3 PGvector Connectivity Smoke Test](version-updates/EXEC_PLAN_V5_A3_PGVECTOR_CONNECTIVITY_SMOKE_TEST.md)
- [V5.A RAG Production Path Completion](version-updates/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md)
- [V5.A RAG Production Path Summary](version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md)


> 📋 [V4 完整口径说明](version-updates/V4_FACTS.md) — V4 completed 的含义、已完成范围、以及仍为 future work 的边界。


## Interview Quick Guide

AfterSale-Agent is an enterprise after-sale ticket Agent platform built with Spring Boot. The review path is designed
to show Agent planning, ToolRegistry-controlled tool execution, risk / approval boundaries, ToolCallTrace audit,
single-run Workspace memory, read-only Execution Tree explanation, RAG policy evidence retrieval, Spring AI adapter
foundation, PGvector / vector repository foundation, OpenAPI docs, Actuator health, and default offline validation.

Core interview points:

- LLMs plan only; Java application code validates plans and tools execute through ToolRegistry.
- Agent does not directly execute high-risk business actions.
- RiskPolicy and Approval protect high-risk proposed actions.
- ToolCallTrace is the audit source of truth for tool calls.
- Workspace is single AgentRun working memory, not long-term memory.
- Execution Tree is a read-only explanation view.
- `search_aftersale_policy` supports KEYWORD, VECTOR, and HYBRID policy evidence retrieval.
- RAG evidence is policy evidence only, not a business action or decision confidence score.
- Spring AI, DashScope / OpenAI, PGvector, JDBC vector persistence, and live embedding providers are opt-in paths.
- Default validation does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, or external
  network.

Interview docs:

- [V4 Interview Demo Checklist](docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md)
- [V4 Project Highlights](docs/demo/V4_PROJECT_HIGHLIGHTS.md)
- [V4 RAG Demo Script](docs/demo/V4_RAG_DEMO_SCRIPT.md)
- [V4 Policy Ingestion Pipeline](docs/demo/V4_POLICY_INGESTION_PIPELINE.md)
- [V4 PGvector Local Setup](docs/demo/V4_PGVECTOR_LOCAL_SETUP.md)
- [Evaluation Docs](docs/evaluation/EVALUATION.md)
- [OpenAPI Docs](docs/api/OPENAPI.md)
- [Validation Commands](docs/quality/VALIDATION_COMMANDS.md)
- [中文项目整改方案](docs/quality/PROJECT_REMEDIATION_PLAN.md)
- [Production Config Template](docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md)
- [V4 Final Completion Record](version-updates/EXEC_PLAN_V4_FINAL_COMPLETION_RECORD.md)
- [V4 Release Summary](version-updates/V4_RELEASE_SUMMARY.md)
- [Project Review Correction Stage 0](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE0.md)
- [Project Review Correction Stage 1](version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE1_PROD_CONFIG_TEMPLATE.md)
- [API Completeness Decision](docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md)
- [Async / Streaming / Batch API Decision](docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md)
- [Spring AI Deepening Decision](docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md)
- [RAG Quality Improvement Decision](docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md)
- [Deployment Hardening Decision](docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md)
- [Deployment Hardening Roadmap](docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md)
- [V5.A.1 JdbcPolicyVectorRepository](version-updates/EXEC_PLAN_V5_A1_JDBC_POLICY_VECTOR_REPOSITORY.md)
- [V5.A.3 PGvector Connectivity Smoke Test](version-updates/EXEC_PLAN_V5_A3_PGVECTOR_CONNECTIVITY_SMOKE_TEST.md)
- [V5.A RAG Production Path Completion](version-updates/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md)
- [V5.A RAG Production Path Summary](version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md)

Fast validation:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

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

OpenAPI / Swagger UI:

```bash
curl http://localhost:8080/v3/api-docs
```

Open `http://localhost:8080/swagger-ui/index.html` or `http://localhost:8080/swagger-ui.html` for interactive API
docs. See [OpenAPI docs](docs/api/OPENAPI.md) for the API groups, evidence-only boundary, health endpoint boundary,
and default offline path. V4.6.4 is API docs polish only; it does not add runtime behavior and does not require live
providers, API keys, Docker, PostgreSQL, PGvector, MySQL, Redis, real LLMs, or real embedding providers.

## Production Config Template

Stage 1 of the project review correction adds a safe production configuration example:

```text
src/main/resources/application-prod.example.yml
```

See [Production Config Template](docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md) for the environment variable groups,
secret placeholder boundary, and default offline validation boundary.

This template is not loaded by default, is not a production deployment manifest, and does not add production auth,
production monitoring, secret-manager integration, live PGvector validation, or real payment / logistics / refund
integrations. Do not commit real API keys, database passwords, tokens, private endpoints, local absolute paths, raw
prompts, or raw datasets. Default validation still does not require real LLMs, API keys, PostgreSQL, PGvector, Docker,
MySQL, Redis, real embedding providers, Spring AI live calls, or external network.

Stage 6 of the project review correction records the deployment hardening route in
[Deployment Hardening Decision](docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md) and
[Deployment Hardening Roadmap](docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md). This stage is documentation-only:
Dockerfile, CI/CD, Kubernetes / Helm, secret manager, production auth/RBAC, production monitoring, live PGvector
validation, and production deployment remain future work.

V5.A.1 adds an explicit opt-in `JdbcPolicyVectorRepository` for the `rag-postgres` / `pgvector` profile. This is an
infrastructure adapter behind `PolicyVectorRepository`, not a new Agent tool, not a public RAG HTTP endpoint, and not a
retrieval algorithm change. Default validation still uses fake / in-memory dependencies and does not connect to
PostgreSQL / PGvector. See
[V5.A.1 JdbcPolicyVectorRepository](version-updates/EXEC_PLAN_V5_A1_JDBC_POLICY_VECTOR_REPOSITORY.md).

V5.A.3 adds an explicit opt-in live PGvector smoke test for the JDBC adapter:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

The smoke uses `AFTERSALE_PGVECTOR_URL`, `AFTERSALE_PGVECTOR_USERNAME`, `AFTERSALE_PGVECTOR_PASSWORD`, and optional
`AFTERSALE_PGVECTOR_SCHEMA`. Missing environment configuration skips the test through assumptions. The smoke uses
fake / fixed vectors and validates SQL connectivity, persistence, lookup, vector search ranking, cleanup, and sanitized
failure messages only. It does not call real LLMs, real embedding providers, Spring AI `VectorStore`, ToolRegistry, or
`search_aftersale_policy`, and it does not validate RAG quality.

V5.A closes the RAG production path foundation through V5.A.1 opt-in JDBC adapter, V5.A.2 schema baseline, V5.A.3
opt-in connectivity smoke, and V5.A.4 docs completion record. See
[V5.A RAG Production Path Completion](version-updates/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md) and
[V5.A RAG Production Path Summary](version-updates/V5_A_RAG_PRODUCTION_PATH_SUMMARY.md). V5.A does not make live PGvector
part of the default gate, does not validate RAG retrieval quality, does not validate real embedding quality, does not
enable Spring AI `VectorStore` production use, and does not add Flyway / Liquibase migration management.

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

Place downloaded raw datasets in your local gitignored raw dataset directory. Keep only local public downloads there;
do not commit raw large files, personal paths, credentials, or private customer data.

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

Stage 2 of the project review correction records the observability hardening decision in
[Observability Hardening Decision](docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md). The current
baseline is MDC / structured logs, ToolCallTrace, ApprovalRequest, Execution Tree, Actuator health, RAG readiness
diagnostics, OpenAPI docs, and offline RAG evaluation metrics. Prometheus, Grafana, OpenTelemetry, collector-based
tracing, production dashboards, provider cost metrics, and external log aggregation remain future / opt-in work.
Default actuator exposure remains limited to `/actuator/health`.

## Core API List

Current HTTP APIs are a demo/backend API surface, not a complete production CRUD platform. Stage 3.1 records this in
[API Completeness Decision](docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md). Stage 3.2 adds bounded
Ticket list/query pagination. Stage 3.3 adds a read-only AgentRun get/status endpoint. Stage 3.4 records the
[Async / Streaming / Batch API Decision](docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md):
current synchronous create/start plus status polling remains the safe path, while async AgentRun runtime,
SSE / WebSocket streaming, batch APIs, cancel / retry, AgentRun list pagination, and production auth / RBAC remain
future follow-ups; they are not completed runtime behavior.

Health:

```bash
GET /api/health
GET /actuator/health
```

Tickets:

```bash
POST /api/tickets
GET /api/tickets?page=0&size=20&sort=createdAt,desc
GET /api/tickets/{ticketId}
```

Ticket list supports bounded pagination and read-only filters for `status`, `userId`, `orderId`, `intentType`,
`createdFrom`, and `createdTo`. It does not start AgentRun execution and does not expose a public RAG search endpoint.

Agent execution:

```bash
POST /api/tickets/{ticketId}/agent-runs
GET /api/agent-runs/{runId}
GET /api/agent-runs/{runId}/traces
GET /api/agent-runs/{runId}/execution-tree
```

`GET /api/agent-runs/{runId}` is a read-only status polling view. It returns safe summary fields and links to trace
and execution-tree endpoints. It does not run the planner, execute tools, call ToolRegistry, write ToolCallTrace,
mutate Workspace, or inline execution-tree details.

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

## V1 能力边界

V1 建立了规则型 AgentRun 闭环：Ticket 创建、规则意图分类、内存策略检索、ToolRegistry 工具执行、AgentRun 记录。
详见 [V1_CAPABILITY_BOUNDARY.md](version-updates/V1_CAPABILITY_BOUNDARY.md)

## V2 Roadmap

V2 引入真实 LLM Planner Adapter，支持多意图规划、订单工具、政策检索工具、审批 API、执行树、Agent Workspace 等。
详见 [V2_ROADMAP.md](version-updates/V2_ROADMAP.md)

## V3 Roadmap

V3 是基础设施闭合阶段：MySQL 持久化、Docker Compose、结构化日志/可观测性、Demo 数据集增强。
详见 [V3_ROADMAP.md](version-updates/V3_ROADMAP.md)


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


## V4 Roadmap

V4 聚焦面试关键的 AI 工程能力：RAG/向量化策略检索、Spring AI Adapter、PGvector、
工具/技能层、执行树证据可视化、Actuator health、OpenAPI docs。
详见 [V4_ROADMAP.md](version-updates/V4_ROADMAP.md)
