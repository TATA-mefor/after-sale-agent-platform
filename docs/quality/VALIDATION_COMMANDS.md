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

## V5.B.2.1 Config / Secret / Migration Plan Validation

V5.B.2.1 adds documentation for the configuration baseline, profile matrix, secret boundary, Docker / CI secret
boundary, and migration follow-up split. It does not modify runtime configuration semantics, Dockerfile, CI workflow,
compose files, `src/main/java`, Flyway / Liquibase, secret manager, ToolRegistry, RAG runtime, ingestion, health, or
OpenAPI behavior.

Targeted docs harness:

```bash
mvn test -Dtest=ConfigSecretMigrationPlanDocsTest
```

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V5.B.2.1 validation is docs-only and offline. It checks that `application.yml` remains documented as the default
offline/local baseline, `application-prod.example.yml` remains template-only, `application-mysql.yml` and
`application-rag-postgres.yml` remain explicit opt-in profiles, Docker and CI do not bake or inject live secrets, and
V5.B.2.2 later adds the Flyway migration foundation while keeping Flyway disabled by default. Liquibase is not
introduced. V5.B.2.3 Profile Matrix Validation later adds the file-based profile matrix validation harness. Default
validation still does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
providers, Spring AI live calls, secret manager, Docker Compose, or external network.

## V5.B.2.2 Flyway Migration Foundation Validation

V5.B.2.2 adds Flyway dependencies, default-disabled configuration, explicit `mysql` and `rag-postgres` migration
locations, and schema-only MySQL / PGvector baseline migration files. It does not run migrations by default and leaves
profile matrix validation harness coverage to V5.B.2.3.

Targeted docs/config harness:

```bash
mvn test -Dtest=FlywayMigrationFoundationDocsTest
```

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

This validation is offline. It reads `pom.xml`, Spring profile config, migration SQL files, and docs only. It does not
start Spring, create a `DataSource`, connect to MySQL / PostgreSQL / PGvector, run Docker, call real LLMs, call real
embedding providers, invoke Spring AI live providers, or use external network.

## V5.B.2.3 Profile Matrix Validation

V5.B.2.3 profile matrix validation harness completed. V5.B.2 current scope completed. The tests read repository files
only and verify default, `mysql`, `rag-postgres`, production template, Flyway, CI, live smoke, and secret boundaries.
Runtime profile behavior was not changed.

Targeted config/docs harness:

```bash
mvn test -Dtest=ProfileMatrixValidationTest
mvn test -Dtest=ProfileMatrixValidationDocsTest
```

The harness verifies:

- default offline / local baseline stays in `application.yml`;
- `application-mysql.yml` uses `AFTERSALE_MYSQL_URL`, `AFTERSALE_MYSQL_USERNAME`,
  `AFTERSALE_MYSQL_PASSWORD`, and `AFTERSALE_FLYWAY_ENABLED:false`;
- `application-rag-postgres.yml` uses `AFTERSALE_PGVECTOR_URL`, `AFTERSALE_PGVECTOR_USERNAME`,
  `AFTERSALE_PGVECTOR_PASSWORD`, `AFTERSALE_PGVECTOR_SCHEMA`, and `AFTERSALE_RAG_FLYWAY_ENABLED:false`;
- `application-prod.example.yml` remains template only;
- Flyway remains disabled by default while profile locations remain `classpath:db/migration/mysql` and
  `classpath:db/migration/pgvector`;
- live PGvector smoke stays explicit opt-in through `-Dlive.rag=true` and sanitized skip behavior, including
  `CREATE EXTENSION IF NOT EXISTS vector` setup limitations.

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Optional live PGvector smoke remains outside the default gate:

```bash
mvn test -Dtest=JdbcPolicyVectorRepositorySmokeTest -Dlive.rag=true
```

Default validation still does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, secret manager, Docker Compose, or external network.

## V5.B.3.1 Readiness / Liveness Boundary Validation

V5.B.3.1 readiness / liveness actuator probe boundary completed. It enables Spring Boot health probes and adds explicit
`liveness` and `readiness` health groups while keeping Actuator web exposure limited to `health`.

Targeted runtime/docs harness:

```bash
mvn test -Dtest=ReadinessLivenessBoundaryTest
mvn test -Dtest=ReadinessLivenessBoundaryDocsTest
```

The runtime test verifies:

- `/actuator/health` is available;
- `/actuator/health/liveness` is available;
- `/actuator/health/readiness` is available;
- `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`, `/actuator/threaddump`, and
  `/actuator/prometheus` remain unavailable by default;
- the default context does not create `DataSource`, Spring AI live model, Spring AI `VectorStore`, or
  `JdbcPolicyVectorRepository` beans.

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V5.B.3.1 validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, secret manager, Docker Compose, Prometheus, OpenTelemetry collector, or
external network.

V5.B.3.2 Micrometer metrics foundation completed. V5.B.3.3 Prometheus opt-in exposure completed. V5.B.3.4 tracing /
correlation boundary completed. V5.B.3.5 observability docs + completion record completed. V5.B.4.3 K8s / Helm Foundation completed; V5.B.4.4 Release / Rollback Foundation
planned. Production monitoring backend, dashboards, alerting, log
aggregation, and OpenTelemetry remain future / opt-in work.

## V5.B.3.2 Micrometer Metrics Foundation Validation

V5.B.3.2 low-cardinality Micrometer metrics foundation completed. It records AgentRun, ToolCall, Approval, RAG search,
and provider-call observations through a centralized recorder while keeping Actuator web exposure limited to health.

Targeted runtime/docs harness:

```bash
mvn test -Dtest=ApplicationMetricsRecorderTest
mvn test -Dtest=MetricsFoundationBoundaryTest
mvn test -Dtest=MetricsFoundationDocsTest
```

The runtime and docs tests verify:

- `MeterRegistry` and `ApplicationMetricsRecorder` exist in the default context;
- project-owned metric names use the `aftersale.*` prefix;
- metric tags stay low-cardinality and sanitize secrets, paths, prompts, queries, snippets, URLs, JDBC URLs, and raw
  free text to `unknown`;
- AgentRun, ToolCall, Approval, RAG search, and provider metrics are recorded best-effort;
- `/actuator/metrics`, `/actuator/prometheus`, `/actuator/env`, `/actuator/beans`, `/actuator/configprops`,
  `/actuator/heapdump`, and `/actuator/threaddump` remain unavailable by default;
- the default context still does not create `DataSource`, Spring AI live model, Spring AI `VectorStore`, or
  `JdbcPolicyVectorRepository` beans.

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V5.B.3.2 validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Spring AI `VectorStore`, secret manager, Docker Compose, Prometheus,
OpenTelemetry collector, or external network.

## V5.B.3.3 Prometheus Opt-in Exposure Validation

V5.B.3.3 Prometheus opt-in exposure completed. It adds the Boot-managed Prometheus registry dependency and the explicit
`observability-prometheus` profile for local `/actuator/prometheus` review while keeping default Actuator web exposure
health-only.

Targeted runtime/docs harness:

```bash
mvn test -Dtest=PrometheusOptInExposureTest
mvn test -Dtest=PrometheusOptInDocsTest
```

The runtime and docs tests verify:

- default `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` remain available;
- default `/actuator/prometheus`, `/actuator/metrics`, `/actuator/env`, `/actuator/beans`, `/actuator/configprops`,
  `/actuator/heapdump`, and `/actuator/threaddump` remain unavailable;
- `observability-prometheus` exposes `/actuator/prometheus` and keeps `/actuator/metrics` plus sensitive endpoints
  unavailable;
- the default context still does not create `DataSource`, Spring AI live model, Spring AI `VectorStore`, or
  `JdbcPolicyVectorRepository` beans;
- OpenTelemetry, distributed tracing, dashboards, scrape jobs, alerts, production monitoring backend, production auth,
  Kubernetes / Helm, release / rollback hardening, and real external business integrations remain future work.

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V5.B.3.3 validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Spring AI `VectorStore`, secret manager, Docker Compose, Prometheus server,
Grafana, OpenTelemetry collector, or external network.

OpenTelemetry tracing, dashboards, production monitoring backend, production auth, Kubernetes / Helm, and release /
rollback hardening remain planned / future work. V5.B.3.4 tracing / correlation boundary completed separately as local
HTTP log correlation.

## V5.B.3.4 Tracing / Correlation Boundary Validation

V5.B.3.4 tracing / correlation boundary completed. It adds safe `X-Correlation-Id` and `X-Request-Id` handling,
response headers, MDC keys `correlationId` and `requestId`, and structured logging support while keeping default
Actuator exposure health-only.

Targeted runtime/docs harness:

```bash
mvn test -Dtest=CorrelationIdsTest
mvn test -Dtest=CorrelationIdFilterBoundaryTest
mvn test -Dtest=CorrelationObservabilityBoundaryTest
mvn test -Dtest=TracingCorrelationDocsTest
```

The runtime and docs tests verify:

- missing or unsafe correlation / request headers are replaced with generated safe values;
- accepted values use safe characters and stay within 128 characters;
- unsafe values are not echoed and are not placed into MDC;
- MDC keys `correlationId` and `requestId` are cleared after request processing;
- `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness` remain available;
- `/actuator/prometheus`, `/actuator/metrics`, `/actuator/env`, `/actuator/beans`, `/actuator/configprops`,
  `/actuator/heapdump`, and `/actuator/threaddump` remain unavailable by default;
- correlation IDs and request IDs are not Micrometer tags;
- OpenTelemetry, distributed tracing, cross-service propagation, tracing backend, production monitoring backend,
  production auth, Kubernetes / Helm, and release / rollback hardening remain future work.

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V5.B.3.4 validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Spring AI `VectorStore`, secret manager, Docker Compose, Prometheus server,
Grafana, OpenTelemetry collector, tracing backend, log aggregation backend, or external network.

## V5.B.3.5 Observability Docs + Completion Validation

V5.B.3.5 observability docs + completion record completed. It consolidates the V5.B.3.1 readiness / liveness,
V5.B.3.2 Micrometer metrics, V5.B.3.3 Prometheus opt-in exposure, and V5.B.3.4 tracing / correlation documentation
into a single observability docs map.

Targeted docs harness:

```bash
mvn test -Dtest=ObservabilityDocsCompletionDocsTest
```

The docs harness verifies:

- V5.B.3.5 observability docs and completion record exist;
- README, validation commands, quality score, deployment roadmap, production config template, remediation plan, and
  V5 status docs link or mention the completion;
- production monitoring backend, Grafana dashboards, alerting, scrape jobs, log aggregation, OpenTelemetry,
  distributed tracing, cross-service propagation, production auth, Kubernetes / Helm, and release / rollback hardening
  remain future / opt-in;
- default validation remains offline and does not require live LLM, API Key, PostgreSQL, PGvector, Docker, MySQL,
  Redis, real embedding provider, Prometheus server, Grafana, OpenTelemetry collector, tracing backend, log
  aggregation backend, or external network;
- docs do not contain local absolute paths or real secret assignments.

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V5.B.3.5 is documentation-only. It does not add runtime observability behavior and does not implement production
monitoring.

V5.B.4 planned auth / Kubernetes / release hardening remains future work.

V5.B.4.3 K8s / Helm Foundation completed. V5.B.4.4 Release / Rollback Foundation
completed. V5.B.4 current scope completed. V5.B Production Hardening current planned scope
completed.

## V5.B.4.3 K8s / Helm Foundation Validation

V5.B.4.3 adds Kubernetes manifest templates and a Helm chart skeleton. This is deployment
manifest foundation only — NOT a production deployment.

Targeted docs harness:

```bash
mvn test -Dtest=K8sHelmFoundationDocsTest
```

The docs harness is file-based only. It reads K8s manifests, Helm chart files, and docs.
It does NOT start Spring, call HTTP endpoints, connect to databases, start Docker, call
real LLMs, call real embedding providers, or access external network.

Optional local commands (NOT part of default Maven gate):

```bash
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
kubectl apply --dry-run=client -f deploy/k8s/
```

These commands require `helm` and `kubectl` installed locally. If not installed, skip them.
Default Maven validation does NOT require helm, kubectl, Kubernetes, Docker, MySQL,
PostgreSQL, PGvector, Redis, real LLMs, real embedding providers, or external network.

V5.B.4.3 does NOT implement release / rollback automation, image registry push,
production ingress, external secret manager, sealed secrets, database StatefulSet,
live PGvector deployment, OAuth2 / OIDC, or production deployment.

## V5.B.4.4 Release / Rollback Foundation Validation

V5.B.4.4 adds release governance and rollback runbook foundation: release checklist,
rollback trigger matrix, image tag policy, Helm/K8s release review policy, post-release
verification, and change/release note templates. This is a governance / runbook foundation
only — NOT release automation, NOT production deployment, NOT executed rollback.

Targeted docs harness:

```bash
mvn test -Dtest=ReleaseRollbackFoundationDocsTest
```

The docs harness is file-based only. It reads the release/rollback foundation doc,
templates, and cross-referenced docs. It does NOT start Spring, call HTTP endpoints,
connect to databases, start Docker, call real LLMs, call real embedding providers, or
access external network.

Optional local commands (NOT part of default Maven gate):

```bash
docker build -t after-sale-agent-platform:local .
helm template after-sale-agent-platform deploy/helm/after-sale-agent-platform
kubectl apply --dry-run=client -f deploy/k8s/
```

These commands require Docker, Helm, and kubectl installed locally. If not installed,
skip them. Default Maven validation does NOT require these tools.

V5.B.4.4 does NOT implement GitHub release workflow, image registry push,
semantic-release, automated version bump, Helm install/upgrade automation,
kubectl apply automation, production deployment, real rollback execution,
secret manager, Argo CD/Flux, or Terraform.

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## V5.B.4.1 Production Auth / RBAC Boundary Validation

V5.B.4.1 Production Auth / RBAC Boundary Decision completed. It is documentation-only and records the current auth
gap, planned RBAC role model, API access matrix, actuator / OpenAPI boundary, Approval boundary, ToolRegistry
boundary, RAG evidence-only boundary, K8s exposure precondition, and release / rollback security precondition.

Targeted docs harness:

```bash
mvn test -Dtest=AuthRbacBoundaryDocsTest
```

The docs harness verifies:

- `docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md` exists and marks V5.B.4.1 completed;
- `docs/deploy/AUTH_RBAC_BOUNDARY.md` exists and states production auth runtime remains planned;
- `docs/exec-plans/completed/EXEC_PLAN_V5_B4_1_AUTH_RBAC_BOUNDARY_DECISION.md` contains `TASK_COMPLETE`;
- README, deployment roadmap, production config template, OpenAPI docs, quality score, remediation plan, validation
  commands, and V5 status docs mention the boundary;
- ToolRegistry direct access is never public;
- high-risk actions require Approval;
- `search_aftersale_policy` remains LOW-risk read-only;
- RAG evidence remains evidence-only policy support;
- V5.B.4.2 Spring Security / API Key Auth Foundation completed, V5.B.4.3 K8s / Helm Foundation planned, and
  V5.B.4.4 Release / Rollback Foundation planned;
- docs do not contain local absolute paths, real secret assignments, or production capability overclaims.

Default Maven gate remains unchanged:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

V5.B.4.1 validation is file-based only. It does not start Spring context, call HTTP endpoints, connect to databases,
run Docker, call real LLMs, call real embedding providers, use Spring AI live providers, use Spring AI `VectorStore`,
or access external network. V5.B.4.2 API key auth foundation is opt-in and covered by targeted tests. Kubernetes /
Helm remains planned for V5.B.4.3. Release / rollback hardening remains planned for V5.B.4.4.

## V5.B.4.2 Spring Security / API Key Auth Foundation Validation

V5.B.4.2 completed the opt-in Spring Security API key auth foundation. The default profile remains permit-all and
offline; `security-api-key` enables `X-API-Key` enforcement.

Targeted validation:

```bash
mvn test -Dtest=SecurityDefaultBoundaryTest
mvn test -Dtest=ApiKeyCredentialValidatorTest
mvn test -Dtest=ApiKeyAuthBoundaryTest
mvn test -Dtest=ApiKeyAuthFoundationDocsTest
```

Additional targeted Prometheus security validation:

```bash
mvn test -Dtest=ApiKeyPrometheusBoundaryTest
```

The docs and security harness verify:

- default profile does not require API key env vars;
- default profile remains permit-all and offline;
- `security-api-key` profile enables stateless Spring Security API key auth;
- `X-API-Key` is the documented header;
- missing or invalid API key returns `401` without echoing raw key values;
- insufficient role returns `403`;
- health probes remain public;
- Ticket / AgentRun / Approval / Trace / ExecutionTree are protected by role boundary;
- OpenAPI / Swagger UI requires `ADMIN` or `SUPERVISOR`;
- opt-in Prometheus requires `ADMIN` or `SYSTEM_SERVICE` when exposed;
- sensitive actuator endpoints remain unexposed;
- OAuth2 / OIDC, JWT issuer / JWKS, session login, user database, secret manager, Kubernetes / Helm, and
  release / rollback automation remain unimplemented.

Default Maven gate remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

The default gate still does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider, Spring AI `VectorStore`, Prometheus server, secret manager or external network.

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
