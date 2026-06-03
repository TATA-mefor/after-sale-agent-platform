# Validation Commands

Status: V4 final default offline validation gate.

This file records the repository validation boundary. The default commands are offline, deterministic, and must not
require real providers or external services.

## Default Commands

Run these commands for normal development and review:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

The default validation path must not require:

- real LLM;
- API Key;
- PostgreSQL;
- PGvector;
- Docker;
- MySQL;
- Redis;
- external network;
- real embedding provider;
- Spring AI live provider calls.

If any default command requires one of those dependencies, treat it as a regression.

## V4 Final Validation

The V4 final default validation gate is:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

This gate is the expected final verification set for V4 completion. It covers default offline tests, docs harness
tests, architecture boundaries, style checks, static analysis, RAG evaluation tests, RAG health tests, OpenAPI docs
tests, and AgentRun regression tests.

Live checks are explicit opt-in and are not part of this default gate. The final V4 default gate does not require API
keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real LLMs, real embedding providers, Spring AI VectorStore, or
external network.

## 项目审查后的事实口径

V4 final default validation gate 证明默认路径离线、确定性、边界可检查；它不证明生产部署、生产认证、生产监控、
live PGvector、真实 provider 或真实外部业务系统可用。

- 当前 observability 已覆盖 MDC / structured logs、ToolCallTrace、Execution Tree、Actuator health 和 RAG readiness
  diagnostics。
- 当前 observability 未覆盖 Prometheus registry、metrics dashboard、distributed tracing 或 cross-service trace-id
  propagation；这些是 V5 / future work。
- 当前 Spring AI 是 adapter foundation，不是 ChatMemory、Advisors、Tool Calling API 或 bulk embedding 深度使用。
- 当前 RAG 支持 KEYWORD / VECTOR / HYBRID policy evidence retrieval，但没有 reranking、query rewriting、RRF 或
  chunk window expansion。
- 当前 API 是 demo/backend API surface，不是完整生产 CRUD 平台。
- PGvector 当前是 foundation / opt-in profile，不是默认 live vector persistence。

中文整改方案见 `docs/quality/PROJECT_REMEDIATION_PLAN.md`。该文档只做项目审查结论的事实核验与阶段化整改路线，
不改变 runtime 行为。对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=ProjectRemediationPlanDocsTest
```

## Production Config Template Validation

阶段 1 新增 `src/main/resources/application-prod.example.yml` 和
`docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md`。这是生产配置模板与说明，不是生产部署方案，不会被默认
profile 自动加载，也不改变 runtime 行为。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
```

该测试只读模板和文档，检查：

- README、整改方案和验证文档链接生产配置模板；
- 模板使用环境变量 placeholder；
- 模板和文档不包含真实 API Key、数据库密码、token、本地绝对路径或 raw dataset path；
- 文档说明默认验证不加载 prod template；
- 文档说明默认验证不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或 external network；
- 文档不把 production auth、production monitoring、secret manager、CI/CD、Kubernetes、Helm 或 live PGvector
  validation 写成已完成能力。

## Observability Hardening Decision Validation

阶段 2 新增 `docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md` 和完成记录。该阶段只做
可观测性决策、文档路线和 docs harness coverage，不接入 runtime monitoring。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
```

该测试只读文档，检查：

- README、中文整改方案、验证文档、生产配置模板说明和 active correction plan 链接或记录阶段 2；
- 当前 baseline 明确为 MDC / structured logs、ToolCallTrace、ApprovalRequest、Execution Tree、Actuator health、
  RAG readiness diagnostics、OpenAPI docs 和 offline RAG evaluation metrics；
- Prometheus、Grafana、OpenTelemetry、collector、dashboard、provider cost metrics 和 external logging platform
  是 future / opt-in，不是当前已接入 runtime；
- 默认 actuator exposure 继续只包含 `/actuator/health`；
- 敏感 actuator endpoints 如 env、beans、configprops、heapdump、threaddump、prometheus 不默认暴露；
- health 不调用真实 LLM、embedding provider、PGvector、Spring AI `VectorStore`、ToolRegistry 或 AgentRun；
- docs 不包含真实 API Key、数据库密码、token、本地绝对路径、raw prompt 或 raw dataset path。

默认验证不需要 Prometheus、Grafana、OpenTelemetry collector、external logging platform、external monitoring
platform 或 external network。如果默认验证需要这些依赖，视为回归。

## API Completeness Decision Validation

阶段 3.1 新增 `docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md` 和完成记录。该阶段只做 API surface
audit、API completeness decision、文档路线和 docs harness coverage，不新增 runtime API。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=ApiCompletenessDecisionDocsTest,ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
```

该测试只读文档，检查：

- 当前 API 明确为 demo/backend API surface，不是完整生产 CRUD；
- Stage 3.1 记录当时 Ticket 只有 create/get；Stage 3.2 已补 `GET /api/tickets` bounded list/query pagination；
- Stage 3.3 已补 `GET /api/agent-runs/{runId}` read-only AgentRun get/status polling；
- ToolCallTrace 和 Execution Tree 是 read-only views；
- Approval API 为 pending/get/approve/reject；
- `/actuator/health`、`/v3/api-docs` 和 Swagger UI 已记录；
- `search_aftersale_policy` 仍是 LOW-risk read-only ToolRegistry tool，不是 public RAG HTTP endpoint；
- 异步 AgentRun、SSE / WebSocket、batch API、production auth / RBAC 是 planned / future；
- 文档不包含真实 API Key、数据库密码、token、本地绝对路径、raw prompt 或 raw dataset path。

阶段 3.1 默认验证不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider、
Spring AI live provider calls 或 external network。如果默认验证需要这些依赖，视为回归。

## Ticket Pagination Foundation Validation

阶段 3.2 新增 `GET /api/tickets` Ticket list/query pagination foundation 和完成记录。该阶段只补 Ticket
只读列表查询，不新增 AgentRun status endpoint，不实现异步 AgentRun、SSE / WebSocket、batch API、production auth /
RBAC，也不新增 public RAG HTTP endpoint。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=TicketPaginationDocsTest,ApiCompletenessDecisionDocsTest
```

该测试只读文档，检查：

- README、OpenAPI docs、API completeness decision、整改方案、quality docs、validation docs 和 active correction plan
  记录 Stage 3.2 completed；
- Ticket API 文档包含 `GET /api/tickets?page=0&size=20&sort=createdAt,desc`；
- Ticket list/query pagination 明确支持 `page`、`size`、`sort`、`status`、`userId`、`orderId`、`intentType`、
  `createdFrom` 和 `createdTo`；
- Ticket list endpoint 是只读查询，不创建 AgentRun，不调用 ToolRegistry，不暴露 public RAG HTTP endpoint；
- async AgentRun、SSE / WebSocket、batch API、production auth / RBAC 仍是 planned / future。

阶段 3.2 默认验证不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider、
Spring AI live provider calls 或 external network。如果默认验证需要这些依赖，视为回归。

## AgentRun Status Read Validation

阶段 3.3 新增 `GET /api/agent-runs/{runId}` read-only AgentRun get/status polling endpoint 和完成记录。该阶段
只补 AgentRun 读取模型，不实现异步 AgentRun、SSE / WebSocket、batch API、production auth / RBAC，也不新增
public RAG HTTP endpoint。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=AgentRunStatusDocsTest,TicketPaginationDocsTest,ApiCompletenessDecisionDocsTest
```

该测试只读文档，检查：

- README、OpenAPI docs、API completeness decision、整改方案、quality docs、validation docs 和 active correction plan
  记录 Stage 3.3 completed；
- AgentRun API 文档包含 `GET /api/agent-runs/{runId}`；
- AgentRun status endpoint 明确只返回安全状态摘要和 trace / execution-tree 链接；
- AgentRun status endpoint 不运行 Planner，不调用 ToolRegistry，不写 ToolCallTrace，不修改 Ticket、Workspace、
  Approval 或 Execution Tree；
- async AgentRun、SSE / WebSocket、batch API、production auth / RBAC 仍是 planned / future。

阶段 3.3 默认验证不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider、
Spring AI live provider calls 或 external network。如果默认验证需要这些依赖，视为回归。

## Async / Streaming / Batch API Decision Validation

阶段 3.4 新增 `docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md` 和完成记录。该阶段只做
async AgentRun、status polling、SSE / WebSocket、batch API、cancel / retry 和 AgentRun list pagination 的决策
评估，不新增 runtime endpoint。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=AsyncStreamingBatchApiDecisionDocsTest,AgentRunStatusDocsTest,TicketPaginationDocsTest,ApiCompletenessDecisionDocsTest
```

该测试只读文档，检查：

- Stage 3.4 decision 文档和完成记录存在并包含 `TASK_COMPLETE`；
- README、OpenAPI docs、API completeness decision、整改方案、quality docs、validation docs 和 active correction plan
  记录 Stage 3.4 completed as decision / evaluation；
- 当前 baseline 包含 Ticket list pagination、AgentRun get/status polling、ToolCallTrace read-only view、
  Execution Tree read-only view、Approval pending/get/approve/reject 和 `search_aftersale_policy` ToolRegistry
  boundary；
- async AgentRun runtime、SSE / WebSocket runtime、batch API runtime、cancel / retry 和 AgentRun list pagination
  仍是 future work；
- streaming 不得暴露 raw prompt、raw LLM response、secrets、full tool output 或完整 evidence chunk；
- batch API 需要 idempotency、rate limit、partial failure model 和 permission boundary；
- production auth / RBAC 是 streaming、batch、cancel / retry 和 production API hardening 前置项；
- docs 不包含真实 API Key、数据库密码、token、本地绝对路径、raw prompt 或 raw dataset path。

阶段 3.4 默认验证不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider、
Spring AI live provider calls、streaming server、queue 或 external network。如果默认验证需要这些依赖，视为回归。

## Spring AI Deepening Decision Validation

阶段 4 新增 `docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md` 和完成记录。该阶段只做
ChatMemory、Advisors、Spring AI Tool Calling API 和 bulk embedding 的深化评估，不修改 runtime provider 行为。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest,ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
```

该测试只读文档，检查：

- Stage 4 decision 文档和完成记录存在并包含 `TASK_COMPLETE`；
- README、V4 Spring AI adapter decision、整改方案、quality docs、validation docs 和 active correction plan 记录
  Stage 4 completed as decision / evaluation；
- 当前 baseline 是 Spring AI Chat adapter foundation、Spring AI embedding adapter foundation、`LlmClient`
  abstraction、`EmbeddingClient` abstraction、`FakeEmbeddingClient` 和 opt-in live smoke tests；
- ChatMemory is not implemented，Advisors are not implemented，Spring AI Tool Calling API is not enabled，
  bulk embedding runtime is not implemented；
- Spring AI Tool Calling API cannot replace ToolRegistry，LLM must not directly execute tools，
  `AgentPlanParser` and `AgentPlanValidator` must not be bypassed，high-risk actions still require Approval；
- bulk embedding must stay behind EmbeddingClient abstraction；
- docs 不包含真实 API Key、数据库密码、token、本地绝对路径、raw prompt 或 raw dataset path。

阶段 4 默认验证不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider、
Spring AI live provider calls、Spring AI VectorStore、ChatMemory store、streaming server、queue 或 external network。
如果默认验证需要这些依赖，视为回归。

## RAG Quality Decision Validation

阶段 5 新增 `docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md` 和完成记录。该阶段只做
reranking、query rewriting、RRF / hybrid scoring、chunk window expansion、provider / PGvector path 的 RAG
quality decision / evaluation，不修改 runtime retrieval algorithm。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=RagQualityDecisionDocsTest,SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest,ObservabilityHardeningDecisionDocsTest
```

该测试只读文档，检查：

- Stage 5 decision 文档和完成记录存在并包含 `TASK_COMPLETE`；
- README、RAG retrieval contract、V4 vector decision、evaluation docs、整改方案、quality docs、validation docs 和
  active correction plan 记录 Stage 5 completed as decision / evaluation；
- 当前 baseline 是 KEYWORD / VECTOR / HYBRID、`RagPolicyEvidenceMergeService`、`EmbeddingClient` abstraction、
  `PolicyVectorRepository` contract、`FakeEmbeddingClient`、`InMemoryPolicyVectorRepository` 和 deterministic RAG
  evaluation；
- no LLM-as-judge by default；
- reranking is not implemented，query rewriting is not implemented，RRF is not implemented，chunk window expansion
  is not implemented；
- Stage 5 completion record keeps the historical note that `JdbcPolicyVectorRepository` was not implemented at that
  time；current V5.A.1 adds an opt-in JDBC adapter, while live PGvector validation and Spring AI VectorStore production
  path are still not completed；
- `search_aftersale_policy` remains LOW-risk read-only ToolRegistry tool，RAG evidence is evidence-only，
  RAG score is not business decision confidence，high-risk actions require Approval，LLM must not directly execute
  tools，future RAG improvements must not bypass ToolRegistry / RiskPolicy / Approval / Trace / Workspace /
  Execution Tree；
- docs 不包含真实 API Key、数据库密码、token、本地绝对路径、raw prompt 或 raw dataset path。

阶段 5 默认验证不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider、
real reranker provider、Spring AI VectorStore、RAG runtime server 或 external network。如果默认验证需要这些依赖，
视为回归。

## Deployment Hardening Decision Validation

阶段 6 新增 `docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md`、
`docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md` 和完成记录。该阶段只做 deployment hardening decision /
roadmap / docs harness coverage，不新增 Dockerfile、CI/CD、Kubernetes / Helm、secret manager、production
auth/RBAC、production monitoring、live PGvector validation 或 runtime deployment changes。

对应 docs harness 可用以下命令单独验证：

```bash
mvn test -Dtest=DeploymentHardeningRoadmapDocsTest,RagQualityDecisionDocsTest,SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest
```

该测试只读文档，检查：

- decision doc 和 roadmap 存在并包含 `TASK_COMPLETE`；
- 当前 baseline 记录 `docker-compose.yml`、`docker-compose-rag.yml`、`.env.rag.example`、
  `application-prod.example.yml`、`application-mysql.yml`、`application-rag-postgres.yml`、Actuator health、
  OpenAPI docs 和 default offline validation；
- Dockerfile is not implemented，CI/CD is not implemented，Kubernetes / Helm is not implemented，
  secret manager is not implemented，production deployment is not completed，live PGvector validation is not
  completed，JdbcPolicyVectorRepository live validation is not completed，production auth/RBAC is not completed，
  production monitoring is not completed；
- roadmap 包含 Dockerfile、CI quality gate、profile matrix、secret management、database migration、PGvector
  deployment、readiness/liveness、observability、security/auth 和 release/rollback checklist；
- README、production config docs、整改方案、quality docs、validation docs、release summary 和 active correction
  plan 记录阶段 6 completed；
- docs 不包含真实 API Key、数据库密码、token、本地绝对路径、raw prompt 或 raw dataset path。

阶段 6 默认验证不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider、
Spring AI live provider calls、secret manager、CI runner、Kubernetes / Helm、Prometheus、Grafana、OpenTelemetry
collector 或 external network。如果默认验证需要这些依赖，视为回归。

## V5.A.1 JdbcPolicyVectorRepository Validation

V5.A.1 adds an explicit opt-in JDBC adapter for `PolicyVectorRepository` under the `rag-postgres` / `pgvector` profile.
It does not change `search_aftersale_policy` retrieval algorithms, does not add a public RAG HTTP endpoint, does not
enable Spring AI `VectorStore`, and does not add live PGvector validation to the default gate.

Targeted validation:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositoryTest
mvn test -Dtest=PgVectorProfileBoundaryTest,DefaultOfflineValidationTest,ArchitectureTest
mvn test -Dtest=JdbcPolicyVectorRepositoryDocsTest
```

The default gate remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V5.A.1 default validation must not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real
embedding providers, Spring AI `VectorStore`, or external network access. Live PGvector smoke validation remains a
separate future / opt-in task.

## V5.A.2 Schema Version Baseline Validation

V5.A.2 adds schema version baseline `2026-06-01-001` to `schema-rag-postgres.sql` and documents how the current schema
is initialized for the opt-in `JdbcPolicyVectorRepository` / PGvector policy evidence search path.

Default validation for this phase is docs/schema-baseline only:

```bash
mvn test -Dtest=SchemaVersionBaselineDocsTest,JdbcPolicyVectorRepositoryDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

This validation does not connect to PostgreSQL / PGvector, does not start Docker, does not call real LLMs or embedding
providers, does not use Spring AI `VectorStore`, and does not require API keys, MySQL, Redis, or external network.

V5.A.2 does not add Flyway / Liquibase. Migration framework work remains pending V5.B.2. Live PGvector connectivity
smoke is handled by V5.A.3 as an explicit opt-in test. The default fake / in-memory vector path remains unchanged.

## V5.A.3 PGvector Connectivity Smoke Validation

V5.A.3 adds a live PGvector smoke test for the explicit opt-in `JdbcPolicyVectorRepository` path. It is not part of
default validation and must be requested directly:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

Required environment variables use the existing project convention:

```text
AFTERSALE_PGVECTOR_URL
AFTERSALE_PGVECTOR_USERNAME
AFTERSALE_PGVECTOR_PASSWORD
AFTERSALE_PGVECTOR_SCHEMA
```

`AFTERSALE_PGVECTOR_SCHEMA` is optional and defaults to `public`. If required configuration is missing, the smoke test
is skipped through JUnit assumptions. Default `mvn test` does not run live PGvector smoke and does not connect to
PostgreSQL / PGvector.

The smoke test executes the current `schema-rag-postgres.sql` against the configured database, writes temporary
`v5a3-smoke-` records, verifies document/chunk/embedding lookup, verifies fixed-vector search ranking, checks duplicate
and invalid-vector error safety, and cleans up only its temporary records. It uses fake / fixed vectors and does not
call real LLMs, real embedding providers, Spring AI `VectorStore`, ToolRegistry, AgentRun, or
`search_aftersale_policy`.

`schema-rag-postgres.sql` starts with `CREATE EXTENSION IF NOT EXISTS vector`. Fresh `docker-compose-rag.yml`
initialization runs this through the PostgreSQL init mount, and many existing PGvector databases already have the
extension installed. If a manually configured database user cannot create the extension, the smoke test skips with a
sanitized setup reason. Flyway / Liquibase migration management remains pending V5.B.2.

V5.A.3 validates SQL connectivity and persistence/search plumbing only. It does not validate RAG quality, real
embedding recall, reranking, query rewriting, RRF, chunk window expansion, Spring AI `VectorStore` production use,
production deployment, or public RAG HTTP endpoints.

## V5.A RAG Production Path Completion Validation

V5.A completed the RAG production path foundation:

- V5.A.1 opt-in `JdbcPolicyVectorRepository`;
- V5.A.2 schema version baseline `2026-06-01-001`;
- V5.A.3 opt-in PGvector connectivity smoke;
- V5.A.4 docs / completion record closure.

Targeted docs harness:

```bash
mvn test -Dtest=RagProductionPathCompletionDocsTest,PgVectorConnectivitySmokeDocsTest,SchemaVersionBaselineDocsTest,JdbcPolicyVectorRepositoryDocsTest
```

Default gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Default validation does not connect PostgreSQL / PGvector and does not run the live smoke. Optional live smoke remains:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

PowerShell:

```powershell
mvn test "-Dtest=JdbcPolicyVectorRepositorySmokeTest" "-Dlive.rag=true"
```

V5.A does not complete production deployment, production auth / RBAC, production monitoring, Flyway / Liquibase
migration management, RAG quality enhancement, real embedding quality validation, Spring AI `VectorStore` production
use, public RAG HTTP endpoints, or real refund / exchange / payment / logistics integrations. V5.B remains planned for
production hardening and migration management.

## V5.B.1 Container + CI Validation

V5.B.1 adds Dockerfile hardening, `.dockerignore` secret-safety exclusions, GitHub Actions Maven quality gate, and a
Docker image build validation job.

Default Maven gate:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Targeted docs harness:

```bash
mvn test -Dtest=ContainerCiHardeningDocsTest
```

Optional local Docker validation:

```bash
docker build -t after-sale-agent-platform:local .
```

Docker build validation is optional for local development and does not replace the Maven quality gate. CI may run
`docker build -t after-sale-agent-platform:ci .`, but it does not push images, log in to a registry, run Docker Compose,
or deploy.

Default CI and default Maven validation do not run live smoke tests. Live LLM, live Spring AI, live embedding, live
PGvector, live MySQL, Redis, and external service validation remain explicit opt-in paths.

## Interview Safe Validation Commands

Use this command set before or during an interview when the goal is to show the repository can be verified locally
without live services:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

These commands exercise unit, integration-style offline, docs harness, quality, and architecture checks. They are the
recommended path for interview demos because they do not require live LLMs, API keys, PostgreSQL, PGvector, Docker,
MySQL, Redis, real embedding providers, or external network. Live validation remains explicit opt-in only.

## Architecture Boundary Coverage

`ArchitectureTest` is the mechanical boundary gate for the modular monolith:

- Planner plans; it does not execute tools.
- ToolRegistry remains the Agent tool execution boundary.
- Skill composes safe capability steps and does not replace ToolRegistry.
- Agent, Handler, and Skill code do not directly depend on repositories, embedding clients, vector repositories,
  PGvector infrastructure, Spring AI model APIs, JDBC, DataSource, OpenAPI config, or Actuator health indicators.
- RAG retrieval is policy evidence only.
- Policy Ingestion is an admin/offline pipeline, not an Agent runtime tool.
- RAG evaluation, RAG health, OpenAPI docs, Workspace, ToolCallTrace, and Execution Tree keep separate boundaries.

## Default Offline Closure

Default Spring context validation checks that the app can start without live infrastructure:

- no `DataSource` bean is required;
- no PGvector live connection is created;
- no Spring AI `ChatModel`, `EmbeddingModel`, or `VectorStore` bean is required;
- no live chat or embedding gateway bean is created;
- `/actuator/health` is an offline readiness signal and does not execute live provider checks.

## Optional Live Validation

Live validation is explicit opt-in and is not part of default `mvn test`.

Examples:

```bash
mvn test -Dtest=LlmPlannerLiveSmokeTest -Dlive.llm=true
mvn test -Dtest=SpringAiLlmClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.llm=true
mvn test -Dtest=SpringAiEmbeddingClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.embedding=true
mvn test -Dtest=RealAgentValidationLiveTest -Dlive.llm=true -Dlive.mysql=true
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

PGvector validation remains an optional live path and must use the explicit `live.rag` opt-in flag. It must not run
during default validation.

## Failure Handling

- If default validation tries to call a real LLM, real embedding provider, database, Docker, Redis, PGvector, or the
  external network, fix the default profile or test boundary.
- If live validation is missing provider credentials or database configuration, it should skip through assumptions or
  fail with a clear setup message.
- Do not commit real API keys, database passwords, tokens, local absolute paths, raw prompts, or raw datasets.

## V4.7.2 Boundary

V4.7.2 closes architecture and offline validation coverage only. It does not add runtime behavior, does not modify
`search_aftersale_policy`, does not change retrieval algorithms, does not modify RAG evaluation, Actuator health,
OpenAPI behavior, ToolRegistry, ToolCallTrace, Workspace, or Execution Tree runtime.

## V4.7.4 Final Closure Boundary

V4.7.4 closes final V4 documentation status only. It adds the final completion record and release summary, then keeps
the same default validation gate and live opt-in boundary. It does not add runtime behavior, does not modify
`search_aftersale_policy`, does not change retrieval algorithms, does not modify RAG evaluation, Actuator health,
OpenAPI behavior, ToolRegistry, ToolCallTrace, Workspace, or Execution Tree runtime.
