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
- [V5.B.1 Container + CI](version-updates/EXEC_PLAN_V5_B1_CONTAINER_CI.md)
- [Container + CI Hardening](docs/deploy/CONTAINER_CI_HARDENING.md)
- [V5.B.2 Config / Secret / Migration Plan](docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md)
- [V5.B.2 Config / Secret Decision](docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md)
- [V5.B.2.1 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B2_1_CONFIG_SECRET_BOUNDARY.md)
- [V5.B.2.2 Flyway Migration Foundation](docs/deploy/MIGRATION_FOUNDATION.md)
- [V5.B.2.2 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B2_2_FLYWAY_MIGRATION_FOUNDATION.md)
- [V5.B.2.3 Profile Matrix Validation](docs/exec-plans/completed/EXEC_PLAN_V5_B2_3_PROFILE_MATRIX_VALIDATION.md)
- [V5.B.3.1 Readiness / Liveness Boundary](docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md)
- [V5.B.3.1 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B3_1_READINESS_LIVENESS_BOUNDARY.md)
- [V5.B.3.2 Micrometer Metrics Foundation](docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md)
- [V5.B.3.2 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B3_2_MICROMETER_METRICS_FOUNDATION.md)
- [V5.B.3.3 Prometheus Opt-in Exposure](docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md)
- [V5.B.3.3 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B3_3_PROMETHEUS_OPT_IN_EXPOSURE.md)
- [V5.B.3.4 Tracing / Correlation Boundary](docs/deploy/OBSERVABILITY_TRACING_CORRELATION.md)
- [V5.B.3.4 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B3_4_TRACING_CORRELATION_BOUNDARY.md)


> 📋 [V4 完整口径说明](version-updates/V4_FACTS.md) — V4 completed 的含义、已完成范围、以及仍为 future work 的边界。



## Interview Quick Guide

详见 [docs/demo/DEMO_INTERVIEW_GUIDE.md](docs/demo/DEMO_INTERVIEW_GUIDE.md)

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
- Correlate local HTTP requests and Agent execution logs with safe `X-Correlation-Id`, `X-Request-Id`, and MDC fields.
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

详见 [docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md](docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md)

## MySQL Profile

详见 [docs/deploy/MYSQL_PROFILE.md](docs/deploy/MYSQL_PROFILE.md)

## Demo Dataset Enrichment

详见 [docs/demo/DEMO_DATASET_ENRICHMENT.md](docs/demo/DEMO_DATASET_ENRICHMENT.md)

## Docker Compose Local Development

详见 [docs/deploy/DOCKER_COMPOSE.md](docs/deploy/DOCKER_COMPOSE.md)

## V5.B.1 Container + CI

V5.B.1 adds a multi-stage Dockerfile, `.dockerignore` secret-safety exclusions, and a GitHub Actions quality gate.
The CI gate runs Maven tests, Checkstyle, SpotBugs, ArchitectureTest, and Docker image build validation. It does not
run live PGvector, live LLM, live Spring AI, Docker Compose, or external service checks, and it does not push an image.

Local Docker build validation is optional:

```bash
docker build -t after-sale-agent-platform:local .
docker run --rm -p 8080:8080 after-sale-agent-platform:local
```

See [Container + CI Hardening](docs/deploy/CONTAINER_CI_HARDENING.md). V5.B.1 is not a production deployment.
V5.B.2.1 config / secret boundary, V5.B.2.2 Flyway migration foundation, and V5.B.2.3 Profile Matrix Validation are
completed. V5.B.2 current scope completed. V5.B.3.1 readiness / liveness actuator probe boundary completed.
V5.B.3.2 Micrometer metrics foundation completed. V5.B.3.3 Prometheus opt-in exposure completed. V5.B.3.4 tracing /
correlation boundary completed. V5.B.3.5 planned production monitoring roadmap and V5.B.4 planned auth / Kubernetes /
release hardening remain future work.

## V5.B.2 Config / Secret / Migration Boundary

V5.B.2.1 documents the configuration baseline, profile matrix, secret boundary, and migration follow-up plan. It keeps
`application.yml` as the default offline / local baseline, keeps `application-prod.example.yml` as a template only, and
records `mysql` and `rag-postgres` as explicit opt-in profiles.

See [Config / Secret / Migration Plan](docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md) and
[Config / Secret Decision](docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md).

V5.B.2.2 adds the [Flyway Migration Foundation](docs/deploy/MIGRATION_FOUNDATION.md): Flyway dependencies,
default-disabled configuration, profile-specific migration locations, and MySQL / PGvector schema-only baseline
migrations. Liquibase is not introduced. Flyway remains disabled by default, and default validation still does not
connect to MySQL, PostgreSQL, PGvector, Docker, Redis, real LLMs, real embedding providers, or external network.

V5.B.2.3 adds a file-based profile matrix validation harness for default, `mysql`, `rag-postgres`, production
template, Flyway, CI, and live smoke boundaries. It verifies `AFTERSALE_FLYWAY_ENABLED:false`,
`AFTERSALE_RAG_FLYWAY_ENABLED:false`, and the existing `AFTERSALE_PGVECTOR_*` variable convention. Runtime profile
behavior was not changed.

V5.B.2 does not implement secret manager, production deployment, production auth, production monitoring, or external
business integrations. Real refund / exchange / payment / logistics integrations are not connected.

## V5.B.3.1 Readiness / Liveness Boundary

V5.B.3.1 enables Spring Boot Actuator health probes and documents the minimal readiness / liveness boundary.
`/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` are available. Actuator web exposure
remains health-only; `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`,
`/actuator/threaddump`, and `/actuator/prometheus` are not exposed by default.

See [Readiness / Liveness Boundary](docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md) and
[V5.B.3.1 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B3_1_READINESS_LIVENESS_BOUNDARY.md).

This is not production monitoring. V5.B.3.1 itself did not add Prometheus, OpenTelemetry, live DB / PGvector / LLM /
embedding readiness checks, production auth, or deployment hardening. The default profile remains offline and does not
create `DataSource`, Spring AI live model, Spring AI `VectorStore`, or `JdbcPolicyVectorRepository` beans.

## V5.B.3.2 Micrometer Metrics Foundation

V5.B.3.2 adds a low-cardinality Micrometer metrics recording foundation for AgentRun, ToolCall, Approval, RAG search,
and provider-call observations. It uses the existing Spring Boot Actuator / Micrometer core dependency and records
meters through a centralized `ApplicationMetricsRecorder`.

See [Micrometer Metrics Foundation](docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md) and
[V5.B.3.2 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B3_2_MICROMETER_METRICS_FOUNDATION.md).

Actuator web exposure remains health-only. `/actuator/metrics` and `/actuator/prometheus` are not exposed by default.
Prometheus registry, OpenTelemetry tracing, dashboards, provider cost metrics, production monitoring backend,
production auth, Kubernetes / Helm, release / rollback hardening, and real external business integrations remain
planned / future work.

## V5.B.3.3 Prometheus Opt-in Exposure

V5.B.3.3 adds the Boot-managed Prometheus registry dependency and an explicit `observability-prometheus` profile for
local `/actuator/prometheus` review. The default profile remains health-only: `/actuator/prometheus`,
`/actuator/metrics`, `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`, and
`/actuator/threaddump` are not exposed by default.

See [V5.B.3.3 Prometheus Opt-in Exposure](docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md) and
[V5.B.3.3 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B3_3_PROMETHEUS_OPT_IN_EXPOSURE.md).

This is not OpenTelemetry tracing and not production monitoring. The opt-in profile does not connect to a Prometheus
server, Grafana, collector, real LLM, real embedding provider, PostgreSQL, PGvector, MySQL, Redis, Docker, or external
network.

## V5.B.3.4 Tracing / Correlation Boundary

V5.B.3.4 adds local HTTP log correlation for `X-Correlation-Id` and `X-Request-Id`. Incoming values are accepted only
when they use safe characters and stay within the length boundary; missing or unsafe values are replaced before they
reach response headers or MDC. The log pattern includes `correlationId` and keeps `requestId`.

See [V5.B.3.4 Tracing / Correlation Boundary](docs/deploy/OBSERVABILITY_TRACING_CORRELATION.md) and
[V5.B.3.4 Completion Record](docs/exec-plans/completed/EXEC_PLAN_V5_B3_4_TRACING_CORRELATION_BOUNDARY.md).

This is local MDC-based correlation only. It is not OpenTelemetry, not distributed tracing, not cross-service
propagation, not production tracing, and not production monitoring. Correlation IDs and request IDs are not used as
metrics tags. The task does not change AgentRun, ToolRegistry, ToolCallTrace, Workspace, Execution Tree, RAG retrieval,
health indicators, OpenAPI behavior, or external business integrations.

## Observability

详见 [docs/OBSERVABILITY.md](docs/OBSERVABILITY.md)

## Core API List

详见 [docs/api/API_LIST.md](docs/api/API_LIST.md)

## Demo Walkthrough

详见 [docs/demo/DEMO_WALKTHROUGH.md](docs/demo/DEMO_WALKTHROUGH.md)

## Validate

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

The default validation path remains offline. It does not require real LLMs, API keys, PostgreSQL, PGvector, Docker,
MySQL, Redis, real embedding providers, Spring AI live calls, registry secrets, or external network access.

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
