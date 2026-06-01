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
阶段 6 完成部署加固路线决策。

## 总体结论

审查中指出的部分问题真实存在，但也有若干表述需要校准：

- 项目不是空的 Spring Boot skeleton；V4 已完成 AgentRun、ToolRegistry、Approval、ToolCallTrace、Workspace、
  Execution Tree、RAG evidence、Actuator health、OpenAPI docs、默认离线验证和 docs harness。
- 项目也不是生产完成态；production auth、production monitoring、production deployment、真实退款、换货、
  支付、物流和补偿系统接入仍是 future work。
- PGvector 当前是 profile、schema、compose、repository contract、fake / in-memory 默认路径和 opt-in boundary；
  `JdbcPolicyVectorRepository`、默认 live PGvector write/search、Spring AI `VectorStore` production path 仍未完成。
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
- 明确 `JdbcPolicyVectorRepository`、live PGvector validation 和 Spring AI VectorStore production path 仍未完成。
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

阶段 0-6 current correction scope completed。后续 production hardening 仍未完成，需要作为 V5 或独立任务继续推进。

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
- JdbcPolicyVectorRepository is not implemented；
- live PGvector validation is not completed；
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
- JdbcPolicyVectorRepository is not implemented；
- production auth/RBAC is not completed；
- production monitoring is not completed。

阶段 6 不实现这些能力，只记录 roadmap 和 checklist。默认测试继续离线，仍不需要 real LLM、API Key、
PostgreSQL、PGvector、Docker、MySQL、Redis、external network、secret manager、CI runner、Kubernetes / Helm、
Prometheus、Grafana 或 OpenTelemetry collector。

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
- `JdbcPolicyVectorRepository`；
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
mvn test -Dtest=ArchitectureTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
```

## Completion Signal

TASK_COMPLETE
