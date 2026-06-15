# 项目整改方案：阶段 0-6 文档事实口径、生产配置、可观测性、API、Spring AI、RAG 与部署路线

Date: 2026-06-01

Status: Completed

## 目的

本文件用于回应项目整体审查中的 Spring Boot、Spring AI、RAG / Tool、API 和部署评价，给出中文事实核验与
阶段化整改路线。阶段 0 完成文档事实口径修正；阶段 1 补充生产配置模板和 secret placeholder 说明；
阶段 2 完成可观测性加固决策；阶段 3.1 完成 API surface audit / API completeness decision；阶段 3.2 完成
Ticket list/query pagination foundation；阶段 3.3 完成 AgentRun get/status polling read model；阶段 3.4 完成
async AgentRun / SSE / WebSocket / batch API / cancel / retry / AgentRun list pagination 的决策评估；阶段 4 完成
Spring AI ChatMemory / Advisors / Tool Calling API / bulk embedding 的深化评估；阶段 5 完成 RAG 检索质量改进评估；
阶段 6 完成部署加固路线决策；V5.A completed RAG production path foundation。V5.A.1 新增显式 opt-in
`JdbcPolicyVectorRepository`，V5.A.2 新增 schema baseline，V5.A.3 新增 opt-in PGvector connectivity smoke，
V5.A.4 新增总完成记录。V5.B.1 完成 container + CI foundation：新增 multi-stage Dockerfile、
`.dockerignore`、GitHub Actions 默认质量门禁和 Docker build validation 文档。V5.B.3.1 完成 readiness /
liveness actuator probe boundary：启用 health probes，增加 liveness/readiness health groups，并保持 Actuator
exposure 只包含 health。V5.B.3.4 完成 local HTTP tracing / correlation boundary：增加安全的 `X-Correlation-Id`
和 `X-Request-Id` 日志关联。V5.B.1、V5.B.3.1 和 V5.B.3.4 仍不是生产部署、CD、registry release、
Kubernetes / Helm、secret manager、production auth、production monitoring、Prometheus 或 OpenTelemetry 集成。
V5.B.4 in progress overall；V5.B.4.1 完成 Production Auth / RBAC Boundary Decision，V5.B.4.2 Spring Security /
API Key Auth Foundation completed，V5.B.4.3 K8s / Helm Foundation completed，V5.B.4.4 Release / Rollback Foundation
completed。V5.B.4 current scope completed。V5.B Production Hardening current planned scope completed。详见
`docs/deploy/PRODUCTION_HARDENING_COMPLETION_SUMMARY.md`。

## 总体结论

审查中指出的部分问题真实存在，但也有若干表述需要校准：

- 项目不是空的 Spring Boot skeleton；V4 已完成 AgentRun、ToolRegistry、Approval、ToolCallTrace、Workspace、
  Execution Tree、RAG evidence、Actuator health、OpenAPI docs、默认离线验证和 docs harness。
- 项目也不是生产完成态；production auth、production monitoring、production deployment、真实退款、换货、
  支付、物流和补偿系统接入仍是 future work。
- PGvector 当前是 profile、schema、compose、repository contract、fake / in-memory 默认路径、opt-in boundary 和
  V5.A.1 显式 opt-in `JdbcPolicyVectorRepository`；默认 live PGvector write/search、Spring AI `VectorStore`
  production path 仍未完成。
- Spring AI 当前是 adapter foundation；阶段 4 已完成深化评估，但 ChatMemory、Advisors、Tool Calling API、
  bulk embedding runtime 仍是后续增强方向。
- RAG evidence 是政策证据，不是业务决策，也不执行任何业务动作；阶段 5 已完成 RAG 检索质量改进评估，
  但 reranking、query rewriting、RRF、chunk window expansion 仍是 future / opt-in。

## 审查结论核验

### Spring Boot 工程

准确：

- 原先缺少 `application-prod.yml` 或生产配置模板；阶段 1 已新增
  `src/main/resources/application-prod.example.yml` 作为不会默认加载的安全示例。
- 生产级 metrics、Prometheus registry、distributed tracing、cross-service trace-id propagation 还未完成。
- 部分核心模型仍偏薄，业务不变量可以继续向 domain 层下沉。

需要修正：

- “所有配置堆在一个 yaml”不准确；当前已有 default、mysql、rag-postgres profile 分离。
- “工程 skeleton”偏重；当前已经具备可运行、可测试、可审计的模块化单体基础。

阶段 1 完成：

- 增加生产配置模板和配置说明。
- 新增 `docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md`，说明环境变量分组、secret safety、默认离线边界和
  non-production boundary。

阶段 2 完成：

- 新增 `docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md`，明确当前 observability baseline、
  metrics strategy、tracing strategy、Actuator exposure strategy、secret safety 和默认离线边界。
- 明确 Prometheus、Grafana、OpenTelemetry、collector、production dashboards、provider cost metrics 和外部日志
  平台仍是 future / opt-in，不是阶段 2 已实现 runtime 能力。

阶段 3+ 建议：

- 针对 Ticket / Order 梳理业务不变量，逐步减少贫血模型倾向。

### Spring AI 使用

准确：

- 当前 Spring AI 使用停留在 provider adapter、chat / embedding abstraction 和默认离线 fake path。
- 未使用 ChatMemory、Advisors、Tool Calling API、bulk embedding。

需要修正：

- 未使用深层 Spring AI 能力是 V4 的有意边界，不是默认路径缺陷。
- 当前没有完成 Spring AI `VectorStore` production path，也没有默认调用真实 embedding provider。

阶段 4 完成：

- 新增 `docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md`。
- 明确当前 Spring AI 是 Spring AI Chat adapter foundation 和 Spring AI embedding adapter foundation。
- 明确 ChatMemory is not implemented、Advisors are not implemented、Spring AI Tool Calling API is not enabled、
  bulk embedding runtime is not implemented。
- 明确 Spring AI Tool Calling API cannot replace ToolRegistry，LLM must not directly execute tools，
  `AgentPlanParser` and `AgentPlanValidator` must not be bypassed，high-risk actions still require Approval。
- 明确 bulk embedding must stay behind EmbeddingClient abstraction。

阶段 5 完成：

- 新增 `docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md`。
- 明确当前 RAG baseline 是 KEYWORD / VECTOR / HYBRID policy evidence retrieval。
- 明确 deterministic RAG evaluation baseline，默认不使用 LLM-as-judge。
- 明确 reranking is not implemented、query rewriting is not implemented、RRF is not implemented、
  chunk window expansion is not implemented。
- 明确 V5.A.1 前 `JdbcPolicyVectorRepository` 尚未完成；当前 V5.A.1 已新增 opt-in JDBC adapter，但 live
  PGvector validation 和 Spring AI VectorStore production path 仍未完成。
- 明确 future RAG quality improvements must not bypass ToolRegistry / RiskPolicy / Approval / Trace / Workspace /
  Execution Tree。

阶段 6+ 建议：

- 在不破坏 ToolRegistry / Approval / Trace 边界的前提下，按独立任务评估 ChatMemory / Advisors runtime。
- 对 bulk embedding、provider retry、rate limit 和 token budget 单独做设计。

### RAG 与 Tool

准确：

- 当前 RAG 检索支持 KEYWORD / VECTOR / HYBRID policy evidence retrieval。
- 缺少 reranking、query rewriting、RRF、chunk window expansion。
- `search_aftersale_policy` 仍是 LOW-risk read-only ToolRegistry tool。

需要修正：

- RAG evidence 不是业务动作，也不是业务决策置信度。
- Policy ingestion 是 admin/offline pipeline，不是 Agent runtime tool。

阶段 1+ 建议：

- 在保持默认离线的前提下，引入检索质量实验：reranking、query rewriting、RRF 和窗口扩展。
- 将 live PGvector validation 作为显式 opt-in，不进入默认 `mvn test`。

### API 调用

准确：

- 当前 API 覆盖 demo/backend surface；Ticket 已补最小分页查询，AgentRun 已补只读 get/status polling；
  异步 AgentRun、SSE / WebSocket 流式输出和批量操作仍未实现。
- OpenAPI 已存在，但不代表新增 public RAG endpoint 或生产 API 完成。

需要修正：

- 当前 API 不应被描述为完整生产 CRUD 平台。
- `search_aftersale_policy` 是 ToolRegistry tool；没有必要为了文档新增 Controller。

阶段 3.1 完成：

- 新增 `docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md`，明确当前 API 是 demo/backend API surface，
  不是完整生产 CRUD。
- 记录当前 API：Ticket create/get、AgentRun create/start、ToolCallTrace read-only view、Execution Tree read-only
  view、Approval pending/get/approve/reject、health 和 OpenAPI docs。
- 明确分页、AgentRun get/status polling、异步 AgentRun、SSE / WebSocket、batch API、production auth / RBAC 是
  后续阶段，不是当前 runtime 能力。
- 明确 `search_aftersale_policy` 是 LOW-risk read-only ToolRegistry tool，不是 public RAG HTTP endpoint。

阶段 3.2 完成：

- 新增 `GET /api/tickets`，支持 `page` / `size` / `sort` 和只读过滤。
- Ticket list/query pagination 使用默认 in-memory repository 可离线验证。
- OpenAPI docs 展示 Ticket list endpoint、分页参数和过滤边界。
- 该 endpoint 不创建 AgentRun，不写 ToolCallTrace / Workspace，不调用 ToolRegistry、RAG、LLM、embedding provider、
  PGvector 或 Spring AI。

阶段 3.3 完成：

- 新增 `GET /api/agent-runs/{runId}`，提供 AgentRun 只读状态摘要。
- 响应只包含 `runId`、`ticketId`、`status`、时间字段、final / failure summary 和 trace / execution-tree 链接。
- 该 endpoint 不运行 Planner，不调用 ToolRegistry，不写 ToolCallTrace，不修改 Ticket、Workspace、Approval 或
  Execution Tree。
- OpenAPI docs 和 docs harness test 记录 AgentRun get/status 已完成；异步 AgentRun 和 streaming 仍是后续任务。

阶段 3.4 完成：

- 新增 `docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md`。
- 明确当前 synchronous create/start + `GET /api/agent-runs/{runId}` status polling 是当前安全路径。
- 评估 async AgentRun、SSE / WebSocket、batch API、cancel / retry 和 AgentRun list pagination，但不实现这些
  runtime 能力。
- 明确 streaming 不得暴露 raw prompt、raw LLM response、secrets、full tool output 或完整 evidence chunk。
- 明确 batch API 需要 idempotency、rate limit、partial failure model、approval backlog control 和权限边界。
- 明确 production auth / RBAC 是 streaming、batch、cancel / retry 和生产 API hardening 的前置项。

### 部署

准确：

- 当前 Docker / Compose 更偏本地开发和演示，不是生产部署方案。
- 缺少 K8s / Helm、CI/CD、secrets 管理、日志采集和生产健康探针策略。

需要修正：

- `docker-compose-rag.yml` 是本地 PGvector infrastructure，不应被表述为生产 RAG 部署。
- V4 completed 不等于生产部署已完成。

阶段 1+ 建议：

- 增加 production profile template。
- 增加 Dockerfile 瘦身、non-root user、healthcheck 和镜像分层策略。
- 后续再评估 K8s / Helm / CI/CD。

## 阶段化整改路线

阶段 0：已完成。文档事实口径修正、中文整改方案、docs harness test。

阶段 1：已完成。生产配置模板、环境变量表、secret placeholder safety、docs harness test。

阶段 2：已完成。可观测性决策。补 metrics / Prometheus / tracing ADR，明确当前保持 MDC-only、Prometheus /
OpenTelemetry 作为 future / opt-in，Actuator 默认只暴露 health。

阶段 3.1：已完成。API surface audit / API completeness decision。明确当前 API 是 demo/backend surface，不是完整
生产 CRUD；当时不实现分页、AgentRun status、异步执行、SSE / WebSocket、batch API 和 production auth / RBAC。

阶段 3.2：已完成。Ticket list/query pagination foundation。补 `GET /api/tickets`、分页参数、状态和业务字段
过滤、OpenAPI 文档和 docs harness test。

阶段 3.3：已完成。AgentRun get/status polling endpoint。补 `GET /api/agent-runs/{runId}` 只读状态摘要，
不执行 Planner、ToolRegistry、RAG、Approval 或 Execution Tree runtime。

阶段 3.4：已完成。异步 AgentRun、SSE / WebSocket、batch API、cancel / retry 和 AgentRun list pagination
评估决策。该阶段不实现 runtime API。

阶段 4：已完成。Spring AI 深化使用评估。评估 ChatMemory、Advisors、Spring AI Tool Calling API 和 bulk
embedding，但不实现这些 runtime 能力，不改变 provider runtime，不让 Spring AI 绕过 ToolRegistry / Approval /
AgentPlan validation。

历史状态记录：阶段 4 收口时“阶段 5：planned”；阶段 5 收口时“阶段 6 planned”；当前阶段 6 已完成。

阶段 5：已完成。RAG 检索质量改进评估。该阶段只做 decision / evaluation / docs harness，不实现 reranking、
query rewriting、RRF、chunk window expansion，不修改 retrieval algorithm，不改变 `search_aftersale_policy`
runtime。

阶段 6：已完成。部署加固路线。补 deployment hardening decision / roadmap，明确 Dockerfile、CI/CD、
Kubernetes / Helm、secret manager、database migration、PGvector deployment、readiness/liveness、observability、
security/auth 和 release/rollback 后续路线；本阶段不实现这些 runtime / deployment 能力。

阶段 0-6 current correction scope completed。V5.A completed RAG production path foundation：V5.A.1 opt-in
JdbcPolicyVectorRepository、V5.A.2 schema baseline、V5.A.3 opt-in PGvector connectivity smoke、V5.A.4 docs /
completion record 均已完成。V5.B.1 completed container + CI foundation：新增 Dockerfile foundation、
`.dockerignore` secret safety、GitHub Actions Maven quality gate 和 Docker build validation。V5.B.2+ 的
config / secret / migration hardening 已推进到 V5.B.2 current scope completed。V5.B.3.1 readiness / liveness
actuator probe boundary completed。V5.B.3.2 Micrometer metrics foundation completed。V5.B.3.3 Prometheus opt-in
exposure completed。V5.B.3.4 tracing / correlation boundary completed。V5.B.3.5 observability docs +
completion record completed。OpenTelemetry、distributed tracing、production monitoring backend、auth、
Kubernetes / Helm、release / rollback 和 production deployment 仍是后续任务。

## 可观测性决策边界

阶段 2 新增 `docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md`。该文档用于把项目审查中的
“缺少 metrics、Prometheus、distributed tracing、cross-service trace-id propagation”转化为可执行策略。

当前基线：

- MDC / structured logs；
- `X-Request-Id`；
- ToolCallTrace；
- ApprovalRequest；
- Execution Tree；
- `/actuator/health`；
- RAG readiness diagnostics；
- OpenAPI docs；
- offline RAG evaluation metrics。

当前缺口：

- Prometheus registry；
- Grafana dashboard；
- OpenTelemetry；
- collector；
- cross-service trace-id propagation；
- provider latency / cost metrics；
- AgentRun / ToolCall / RAG search production metrics；
- external logging platform。

阶段 2 不实现这些 runtime 能力，只定义 future / opt-in 策略。默认 actuator exposure 继续只包含 `health`，
不默认暴露 env、beans、configprops、heapdump、threaddump 或 prometheus。Health 仍是 offline readiness signal，
不是 live provider 或 live PGvector 连通性证明。

## API 完整性决策边界

阶段 3.1 新增 `docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md`。该文档用于把项目审查中的
“缺少分页、异步 AgentRun、SSE / WebSocket、批量 API”转化为可执行路线。

当前 API baseline：

- Ticket create/get/list with bounded pagination；
- AgentRun create/start/read status；
- ToolCallTrace read-only view；
- Execution Tree read-only view；
- Approval pending/get/approve/reject；
- `/api/health`、`/actuator/health`、`/v3/api-docs` 和 Swagger UI。

当前缺口：

- production-grade async AgentRun；
- SSE / WebSocket trace streaming；
- batch API；
- cancel / retry API；
- AgentRun list pagination；
- production auth / RBAC；
- idempotency、rate limit 和 API audit hardening。

阶段 3.1 不实现这些 runtime 能力，只定义路线。阶段 3.2 已补 Ticket list/query pagination foundation。
阶段 3.3 已补 AgentRun get/status polling read model。阶段 3.4 已补 async / streaming / batch API evaluation
decision，并明确这些能力仍未进入 runtime。
`search_aftersale_policy` 继续是 LOW-risk read-only ToolRegistry tool，不是 public RAG HTTP endpoint。
OpenAPI docs 继续记录 existing API surface，不代表 production API hardening。

## Spring AI 深化评估边界

阶段 4 新增 `docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md`。该文档用于把项目审查中的
“Spring AI 只用了浅层能力”转化为可执行路线。

当前 baseline：

- Spring AI Chat adapter foundation；
- Spring AI embedding adapter foundation；
- `LlmClient` abstraction；
- `EmbeddingClient` abstraction；
- `FakeEmbeddingClient`；
- live Spring AI smoke tests are opt-in。

当前缺口：

- ChatMemory is not implemented；
- Advisors are not implemented；
- Spring AI Tool Calling API is not enabled；
- bulk embedding runtime is not implemented。

阶段 4 不实现这些 runtime 能力，只定义 future / opt-in 策略。Spring AI Tool Calling API cannot replace
ToolRegistry，LLM must not directly execute tools，`AgentPlanParser` and `AgentPlanValidator` must not be bypassed，
high-risk actions still require Approval，bulk embedding must stay behind EmbeddingClient abstraction。

## RAG 检索质量评估边界

阶段 5 新增 `docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md`。该文档用于把项目审查中的
“缺少 reranking、query rewriting、RRF、chunk window expansion”转化为可验证路线。

当前 baseline：

- KEYWORD / VECTOR / HYBRID policy evidence retrieval；
- `RagPolicyEvidenceMergeService`；
- `EmbeddingClient` abstraction；
- `PolicyVectorRepository` contract；
- `FakeEmbeddingClient`；
- `InMemoryPolicyVectorRepository`；
- deterministic RAG evaluation；
- no LLM-as-judge by default。

当前缺口：

- reranking is not implemented；
- query rewriting is not implemented；
- RRF is not implemented；
- chunk window expansion is not implemented；
- live PGvector validation is not completed；
- JdbcPolicyVectorRepository live validation is not completed；
- Spring AI VectorStore production path is not enabled。

阶段 5 不实现这些 runtime 能力，只定义 future / opt-in 策略。`search_aftersale_policy` remains LOW-risk
read-only ToolRegistry tool，RAG evidence is evidence-only，RAG score is not business decision confidence，
high-risk actions require Approval，LLM must not directly execute tools，future RAG improvements must not bypass
ToolRegistry / RiskPolicy / Approval / Trace / Workspace / Execution Tree。

## 部署加固路线边界

阶段 6 新增 `docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md` 和
`docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md`。该阶段用于把项目审查中的“缺少 Dockerfile、CI/CD、Kubernetes /
Helm、secret manager、生产监控、readiness/liveness 和 release/rollback”转化为可执行路线。

当前 baseline：

- `docker-compose.yml`；
- `docker-compose-rag.yml`；
- `.env.rag.example`；
- `application-prod.example.yml`；
- `application-mysql.yml`；
- `application-rag-postgres.yml`；
- Actuator health；
- OpenAPI docs；
- 默认离线验证命令。

当前缺口：

- Dockerfile is not implemented；
- CI/CD is not implemented；
- Kubernetes / Helm is not implemented；
- secret manager is not implemented；
- production deployment is not completed；
- live PGvector validation is not completed；
- JdbcPolicyVectorRepository live validation is not completed；
- production auth/RBAC is not completed；
- production monitoring is not completed。

阶段 6 不实现这些能力，只记录 roadmap 和 checklist。默认测试继续离线，仍不需要 real LLM、API Key、
PostgreSQL、PGvector、Docker、MySQL、Redis、external network、secret manager、CI runner、Kubernetes / Helm、
Prometheus、Grafana 或 OpenTelemetry collector。

## V5.B.1 Container + CI Foundation 边界

V5.B.1 新增 container + CI foundation，用于把阶段 6 的部署加固路线推进到最小可验证基础。

已完成范围：

- `Dockerfile`：multi-stage Maven build + Eclipse Temurin Java 17 JRE runtime；
- runtime image 使用非 root `aftersale` 用户；
- `.dockerignore` 排除 `.env`、key / cert、Git metadata、target、IDE、日志、临时和本地数据文件；
- `.github/workflows/ci.yml` 运行默认 Maven quality gate；
- CI 增加 `docker build -t after-sale-agent-platform:ci .` 验证；
- `docs/deploy/CONTAINER_CI_HARDENING.md` 记录 Dockerfile、CI、secret safety 和默认离线边界；
- `version-updates/EXEC_PLAN_V5_B1_CONTAINER_CI.md` 记录完成信号。

V5.B.1 不完成：

- production deployment；
- CD / release automation；
- registry push；
- Kubernetes / Helm；
- secret manager integration；
- Flyway / Liquibase migration management；
- production auth / RBAC；
- production monitoring；
- Prometheus / OpenTelemetry runtime instrumentation；
- readiness / liveness runtime changes；
- live PGvector validation in the default gate；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

默认 Maven 验证仍不需要 Docker。Docker build validation 是 CI / maintainer 可执行的容器构建检查，不改变
Spring Boot runtime、ToolRegistry、RAG retrieval、health indicator 或 OpenAPI runtime 行为。

## V5.B.2.1 Config + Secret Boundary / Profile Matrix Plan 边界

V5.B.2.1 完成配置、密钥和迁移治理的文档基线。它只做 documentation-first correction，不修改 runtime。

已完成范围：

- 新增 `docs/deploy/CONFIG_SECRET_MIGRATION_PLAN.md`；
- 新增 `docs/decisions/DECISION_V5_B2_CONFIG_SECRET_MIGRATION.md`；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B2_1_CONFIG_SECRET_BOUNDARY.md`；
- 明确 `application.yml` 是 default offline / local baseline；
- 明确 `application-prod.example.yml` 是模板，不默认加载；
- 明确 `application-mysql.yml` 和 `application-rag-postgres.yml` 是显式 opt-in profiles；
- 明确 Docker image 不包含 secret，CI default gate 不注入 live secrets；
- 明确 V5.B.2.2 后续完成 Flyway migration foundation，Liquibase 未引入，V5.B.2.3 后续完成 profile matrix
  validation harness。

V5.B.2.1 不完成：

- secret manager；
- Liquibase；
- Flyway 默认启用或 migration runtime validation；
- runtime profile behavior changes；
- readiness / liveness runtime changes；
- production deployment；
- production auth / RBAC；
- production monitoring；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

默认 Maven 验证仍不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding
provider、Spring AI live calls、secret manager、Docker Compose 或 external network。

## V5.A.1 JdbcPolicyVectorRepository 边界

V5.A.1 新增 `JdbcPolicyVectorRepository`，作为 `PolicyVectorRepository` 的显式 opt-in PostgreSQL / PGvector
infrastructure adapter。

当前 baseline：

- 仅在 `rag-postgres` profile、`agent.rag.vector-store.provider=pgvector` 和
  `agent.rag.vector-store.pgvector.enabled=true` 同时满足时创建；
- 使用 `NamedParameterJdbcOperations`；
- 复用既有 policy vector schema；
- 默认 profile 不创建 `DataSource`，不创建 `JdbcPolicyVectorRepository`，不连接 PostgreSQL / PGvector；
- repository failure 通过 sanitized exception 暴露，不泄露 JDBC URL、密码、raw SQL、raw vector、raw prompt 或
  raw dataset path。

非目标：

- 不修改 `search_aftersale_policy` runtime；
- 不修改 retrieval algorithm；
- 不新增 public RAG HTTP endpoint；
- 不启用 Spring AI `VectorStore` production path；
- 不新增 live PGvector validation；
- 不新增 Flyway / Liquibase migration baseline；
- 不新增 Admin ingestion API。

## V5.A RAG Production Path Foundation 边界

V5.A completed 表示 RAG production path foundation 已收口，不表示整个平台 production-ready。

已完成范围：

- V5.A.1：`JdbcPolicyVectorRepository` 作为显式 opt-in JDBC PGvector adapter；
- V5.A.2：`schema-rag-postgres.sql` schema version baseline `2026-06-01-001`；
- V5.A.3：`JdbcPolicyVectorRepositorySmokeTest` opt-in PGvector connectivity smoke；
- V5.A.4：`version-updates/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md` 总完成记录。

默认路径仍使用 fake / in-memory dependencies，不连接 PostgreSQL / PGvector，不运行 live smoke，不调用真实
LLM、真实 embedding provider 或 Spring AI `VectorStore`。

V5.A 不完成：

- production deployment；
- production auth / RBAC；
- production monitoring；
- Flyway / Liquibase migration management；
- RAG quality enhancement；
- real embedding quality validation；
- Spring AI `VectorStore` production path；
- production ingestion API / admin UI；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

## V5.B Production Hardening 当前状态

当前 V5.B 状态：

- V5.B.1：已完成。Container + CI foundation。
- V5.B.2.1：已完成。Config + Secret Boundary / Profile Matrix Plan。
- V5.B.2.2：已完成。Flyway migration foundation；Liquibase 未引入。
- V5.B.2.3：已完成。V5.B.2.3 Profile Matrix Validation；profile matrix validation harness completed。
- V5.B.3.1：已完成。Readiness / Liveness Boundary；readiness / liveness actuator probe boundary completed。
- V5.B.3.2：已完成。Micrometer low-cardinality metrics foundation；`/actuator/metrics` 和
  `/actuator/prometheus` 仍默认不暴露。
- V5.B.3.3：已完成。Prometheus opt-in exposure；`observability-prometheus` profile 显式开启
  `/actuator/prometheus`，默认 Actuator exposure 仍为 health-only。
- V5.B.3.4：已完成。Tracing / correlation boundary；local HTTP log correlation completed。
- V5.B.3.5：已完成。Observability docs + completion record；production monitoring backend 仍未实现。
- V5.B.4：in progress overall。Auth、Kubernetes / Helm、release / rollback hardening。
- V5.B.4.1：已完成。Production Auth / RBAC Boundary Decision；只完成 documentation-first boundary decision。
- V5.B.4.2：completed。Spring Security / API Key Auth Foundation；默认 profile permit-all，`security-api-key`
  profile opt-in。
- V5.B.4.3：completed。K8s / Helm Foundation。
- V5.B.4.4：completed。Release / Rollback Foundation。
- V5.B.4 current scope completed。

V5.B.1 的完成不等于 V5.B overall completed，也不表示 production deployment 已完成。

## V5.B.3.1 Readiness / Liveness Boundary

V5.B.3.1 完成最小 Actuator readiness / liveness probe 边界。

已完成范围：

- `application.yml` 启用 `management.endpoint.health.probes.enabled=true`；
- 增加 `liveness` 和 `readiness` health groups；
- Actuator web exposure remains health-only；
- `/actuator/health`、`/actuator/health/liveness`、`/actuator/health/readiness` 可用；
- `/actuator/env`、`/actuator/beans`、`/actuator/configprops`、`/actuator/heapdump`、
  `/actuator/threaddump` 和 `/actuator/prometheus` 默认不可用；
- 新增 `ReadinessLivenessBoundaryTest` 和 `ReadinessLivenessBoundaryDocsTest`；
- 新增 `docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md`；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B3_1_READINESS_LIVENESS_BOUNDARY.md`。

V5.B.3.1 不完成：

- Micrometer business metrics；
- Prometheus registry 或 `/actuator/prometheus`；
- Grafana dashboard；
- OpenTelemetry 或 collector configuration；
- production monitoring；
- live DB / PGvector / LLM / embedding readiness checks；
- production auth / RBAC；
- Kubernetes / Helm；
- release / rollback hardening；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

默认验证仍不需要 real LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、real embedding provider、
Spring AI live calls、secret manager、Docker Compose、Prometheus、OpenTelemetry collector 或 external network。

## V5.B.3.2 Micrometer Metrics Foundation

V5.B.3.2 完成低基数 Micrometer metrics foundation。该阶段新增项目自有 metrics recorder，用于记录 AgentRun、
ToolCall、Approval、RAG search 和 provider-call 观测值，但不引入 Prometheus registry、OpenTelemetry、
dashboard 或 production monitoring backend。

已完成范围：

- 新增 `ApplicationMetricsRecorder`、metric names、tag vocabulary 和 tag sanitizer；
- AgentRun / ToolCall / Approval / RAG search 路径接入 best-effort metrics recording；
- provider metrics 作为 foundation hook，不调用真实 provider；
- `/actuator/metrics`、`/actuator/prometheus`、`/actuator/env`、`/actuator/beans`、`/actuator/configprops`、
  `/actuator/heapdump` 和 `/actuator/threaddump` 默认不可用；
- 新增 `ApplicationMetricsRecorderTest`、`MetricsFoundationBoundaryTest` 和 `MetricsFoundationDocsTest`；
- 新增 `docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md`；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B3_2_MICROMETER_METRICS_FOUNDATION.md`。

V5.B.3.2 不完成：

- Prometheus registry 或 `/actuator/prometheus`；
- OpenTelemetry、collector、distributed tracing 或 cross-service propagation；
- Grafana dashboard 或 production monitoring backend；
- provider cost dashboard；
- production auth / RBAC；
- Kubernetes / Helm；
- release / rollback hardening；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

## V5.B.3.3 Prometheus Opt-in Exposure

V5.B.3.3 完成 Prometheus opt-in exposure。该阶段新增 Boot-managed Prometheus registry dependency 和
`observability-prometheus` profile，但不改变默认 health-only exposure。

已完成范围：

- `pom.xml` 新增 `micrometer-registry-prometheus`，版本由 Spring Boot dependency management 管理；
- `application.yml` 默认禁用 Prometheus endpoint；
- `application-observability-prometheus.yml` 显式 profile 暴露 `/actuator/prometheus`；
- `/actuator/metrics`、`/actuator/env`、`/actuator/beans`、`/actuator/configprops`、`/actuator/heapdump` 和
  `/actuator/threaddump` 仍不暴露；
- 新增 `PrometheusOptInExposureTest` 和 `PrometheusOptInDocsTest`；
- 新增 `docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md`；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B3_3_PROMETHEUS_OPT_IN_EXPOSURE.md`。

V5.B.3.3 不完成：

- OpenTelemetry、collector、distributed tracing 或 cross-service propagation；
- Grafana dashboard、scrape jobs、alert rules 或 production monitoring backend；
- provider cost dashboard；
- production auth / RBAC；
- Kubernetes / Helm；
- release / rollback hardening；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

## V5.B.3.4 Tracing / Correlation Boundary

V5.B.3.4 完成 local HTTP tracing / correlation boundary。该阶段新增安全的 `X-Correlation-Id` 和
`X-Request-Id` 处理，把 `correlationId` 和 `requestId` 写入 MDC，并让默认日志 pattern 带上这两个字段。

已完成范围：

- 支持 `X-Correlation-Id` 和 `X-Request-Id` 请求 / 响应头；
- 缺失或 unsafe header value 时生成本地安全 ID；
- safe characters 限定为 `[A-Za-z0-9._:-]`，最大长度为 `128`；
- unsafe header values 不回显、不进入 MDC；
- MDC 仅新增 `correlationId` 和 `requestId`，并在请求结束时清理；
- Actuator exposure 仍保持 health-only；
- metrics tags 不允许使用 `correlationId` 或 `requestId`；
- 新增 `docs/deploy/OBSERVABILITY_TRACING_CORRELATION.md`；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B3_4_TRACING_CORRELATION_BOUNDARY.md`。

V5.B.3.4 不完成：

- OpenTelemetry SDK、span、exporter 或 collector；
- W3C `traceparent`、distributed tracing 或 cross-service propagation；
- Grafana dashboard、scrape jobs、alert rules 或 production monitoring backend；
- provider cost dashboard；
- production auth / RBAC；
- Kubernetes / Helm；
- release / rollback hardening；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

## V5.B.3.5 Observability Docs + Completion Record

V5.B.3.5 完成 observability docs + completion record。该阶段把 V5.B.3.1 readiness / liveness、
V5.B.3.2 Micrometer metrics foundation、V5.B.3.3 Prometheus opt-in exposure 和 V5.B.3.4 tracing /
correlation boundary 串成一个文档收口视图。

已完成范围：

- 新增 `docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md`；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B3_5_OBSERVABILITY_DOCS_COMPLETION_RECORD.md`；
- 更新 README、validation commands、quality score、deployment hardening roadmap、production config template、
  remediation plan 和 V5 状态文档；
- 新增 `ObservabilityDocsCompletionDocsTest`；
- 明确 production monitoring backend、Grafana dashboard、alerting、scrape jobs、log aggregation、
  OpenTelemetry、distributed tracing、cross-service propagation、production auth、Kubernetes / Helm 和
  release / rollback hardening 仍是 future / opt-in。

V5.B.3.5 不完成：

- production monitoring backend；
- Grafana dashboard、alert rules、scrape jobs 或 recording rules；
- external log aggregation；
- OpenTelemetry SDK、exporter、collector、tracing backend 或 production tracing；
- W3C `traceparent` 或 cross-service propagation；
- production auth / RBAC；
- Kubernetes / Helm；
- release / rollback hardening；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

## V5.B.4.1 Production Auth / RBAC Boundary Decision

V5.B.4.1 完成 production auth / RBAC 边界决策。该阶段只修正文档事实口径和后续安全路线，不实现 runtime
认证授权。

已完成范围：

- 新增 `docs/decisions/DECISION_V5_B4_AUTH_RBAC_BOUNDARY.md`；
- 新增 `docs/deploy/AUTH_RBAC_BOUNDARY.md`；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V5_B4_1_AUTH_RBAC_BOUNDARY_DECISION.md`；
- 记录当前 API surface 和 current auth gap；
- 记录 planned RBAC roles：`CUSTOMER`、`AGENT_OPERATOR`、`SUPERVISOR`、`ADMIN`、`SYSTEM_SERVICE`；
- 记录 Ticket、AgentRun、Approval、ToolCallTrace、Execution Tree、Health、OpenAPI / Swagger UI、
  Prometheus opt-in endpoint、future admin ingestion API 和 ToolRegistry direct access 的 API access matrix；
- 明确 ToolRegistry direct access never public；
- 明确 high-risk actions require Approval；
- 明确 `search_aftersale_policy` remains LOW-risk read-only；
- 明确 RAG evidence-only，不是业务决策，也不执行业务动作；
- 明确 K8s / Helm / Ingress exposure 必须等待 runtime auth；
- 明确 release / rollback gate 后续应包含 auth configuration、actuator exposure、secret injection 和 migration
  safety checks。

V5.B.4.1 不完成：

- Spring Security runtime；
- production auth / RBAC runtime；
- JWT、API key runtime、OAuth2 / OIDC 或 session login；
- Kubernetes / Helm；
- release / rollback automation；
- production deployment；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

后续 V5.B.4.2 已完成 opt-in Spring Security / API Key Auth Foundation；这仍不等于 full production IAM。

## 生产配置模板边界

阶段 1 新增的 `src/main/resources/application-prod.example.yml` 是示例模板，不是默认 `prod` 配置文件。
它不会被默认测试 profile 加载，也不代表生产部署完成。真实环境配置值应由部署系统、外部配置中心、
未提交的本地配置或环境变量注入。

模板覆盖：

- Spring Boot 基础服务配置；
- DataSource / Hikari 占位；
- LLM / Spring AI provider 占位；
- RAG / PGvector opt-in 占位；
- Actuator 仅暴露 health 的安全边界；
- Swagger UI 默认关闭的生产模板边界。

模板不提供：

- production authentication / authorization；
- secret manager 集成；
- Prometheus / metrics dashboard；
- distributed tracing；
- CI/CD；
- Kubernetes / Helm；
- Dockerfile hardening；
- live PGvector validation；
- production ingestion API / admin UI；
- 真实退款、换货、优惠券补偿、支付或物流系统接入。

## 默认离线边界

默认验证仍不需要：

- real LLM；
- API Key；
- PostgreSQL；
- PGvector；
- Docker；
- MySQL；
- Redis；
- external network；
- real embedding provider；
- Spring AI live provider calls。
- Prometheus；
- Grafana；
- OpenTelemetry collector；
- external logging platform。

## 验证命令

```bash
mvn test -Dtest=ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
mvn test -Dtest=ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
mvn test -Dtest=ApiCompletenessDecisionDocsTest,ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
mvn test -Dtest=SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest,ObservabilityHardeningDecisionDocsTest,ProductionConfigTemplateDocsTest,ProjectRemediationPlanDocsTest
mvn test -Dtest=RagQualityDecisionDocsTest,SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest,ObservabilityHardeningDecisionDocsTest
mvn test -Dtest=DeploymentHardeningRoadmapDocsTest,RagQualityDecisionDocsTest,SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest
mvn test -Dtest=ProjectRemediationPlanDocsTest
mvn test -Dtest=ContainerCiHardeningDocsTest
mvn test -Dtest=AuthRbacBoundaryDocsTest
mvn test -Dtest=ArchitectureTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
```

## Completion Signal

TASK_COMPLETE
