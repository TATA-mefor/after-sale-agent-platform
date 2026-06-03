# AfterSale-Agent 项目审查问题修正方案

状态：阶段 0-6 已完成，current correction scope completed，V5.A RAG production path foundation completed。V5.A.1 opt-in JdbcPolicyVectorRepository 已完成，V5.A.2 schema init / version baseline 已完成，V5.A.3 PGvector connectivity smoke 已完成，V5.A.4 docs / completion record 已完成，V5.B planned 历史口径已推进为 V5.B.1 Container + CI foundation 已完成，V5.B.2.1 Config + Secret Boundary 已完成，V5.B.2.2 Flyway migration foundation 已完成，V5.B.2.3 planned

历史状态记录：阶段 5 收口时状态为“状态：阶段 0-5 已完成，阶段 6 planned”；当前状态已推进为阶段 0-6 已完成。

历史状态记录：阶段 4 收口时状态为“状态：阶段 0-4 已完成”；当前状态已推进为阶段 0-5 已完成。

历史状态记录：阶段 3.4 收口时状态为“状态：阶段 0-3.4 已完成”；当前状态已推进为阶段 0-4 已完成。

## 1. 目标

本方案把最近一次整体项目审查转化为可执行的修正路径。

那份审查有参考价值，但其中有几类结论需要结合仓库事实重新校准：

- 有些问题是真实存在的工程缺口，应该进入后续修复任务；
- 有些表述偏重或已经过时，应该先修正文档口径；
- 有些事项是 V4 有意不做的边界，应作为 future work，而不是隐藏缺陷。

本方案是 documentation-first。除非后续任务明确批准，否则本阶段不修改 runtime 行为。

## 2. 已核查结论

### 2.1 确实存在的问题

- 生产配置不完整：当前没有 `application-prod.yml` 或生产 profile 模板。
- 默认 `application.yml` 较重，集中放了多类平台配置；虽然已经拆出 `mysql` 和 `rag-postgres` profile，但默认配置仍可继续拆分。
- 可观测性目前主要是 MDC 结构化日志、Actuator health、RAG readiness diagnostics。
- 当前没有 Prometheus registry、metrics dashboard、distributed tracing 或跨服务 trace-id 传播。
- Spring AI 当前使用停留在 adapter 层：单轮 chat completion 和单文本 embedding。
- 当前没有使用 Spring AI ChatMemory、Advisors、Tool Calling API 或 bulk embedding。
- RAG search 已支持 KEYWORD / VECTOR / HYBRID，但还没有 reranking、query rewriting、RRF 或 chunk window expansion。
- HTTP API 当前已有 Ticket list/query pagination 和 AgentRun get/status polling；异步 AgentRun、
  SSE/WebSocket 流式输出和批量 API 仍未实现。
- 部署能力偏本地开发：没有 Kubernetes、Helm、CI/CD workflow、生产级 secret 管理、生产监控或部署加固。

### 2.2 需要修正口径的问题

- 项目不是空的 Spring Boot skeleton。Ticket、AgentRun、Approval、ToolCallTrace、Workspace、Execution Tree、RAG evaluation、Actuator health、OpenAPI docs 都已经存在。
- Ticket 不是纯贫血模型；它有状态流转和 terminal-state guard。Order 更薄，更接近只读模型。
- 当前项目没有实现手写 SQL PGvector repository。默认 vector repository 是 in-memory / fake，PGvector 当前是 profile、schema、compose、docs 和架构边界基础。
- `docker-compose-rag.yml` 提供 PGvector 本地基础设施，但当前没有在同一个 compose 文件里启动 app 服务。
- 当前 REST API 不是完整 CRUD。Ticket 有 create/get/list pagination；AgentRun 有 create，以及 trace /
  execution-tree 只读视图；Approval 有 pending/get/approve/reject。
- V4 completed 不等于 production deployment、production monitoring、production auth、live PGvector validation，也不等于接入真实退款、换货、支付或物流系统。

### 2.3 必须保留的 V4 边界

- 默认测试必须继续离线、确定性。
- live LLM、live embedding、live PGvector、MySQL、Docker、Redis、外部网络检查必须继续显式 opt-in。
- RAG evidence 只是政策证据，不是业务决策，也不执行业务动作。
- `search_aftersale_policy` 仍然是 LOW-risk read-only ToolRegistry tool。
- ToolRegistry 仍然是 Agent tool execution entry。
- Skill 不替代 ToolRegistry。
- Policy ingestion 仍然是 admin/offline pipeline，不是 Agent runtime tool。

## 3. 修正策略

### 阶段 0：文档事实口径修正

范围：

- 修正 V4 文档中把 PGvector / VectorStore 写成已完成 live persistence 的过度表述。
- 更新 README 和质量文档，区分“已完成 foundation”和“后续 production hardening”。
- 修正 API 口径：当前是 demo/backend API surface，不是完整 CRUD。
- 修正 compose 口径：MySQL compose 是 app + MySQL；RAG compose 当前只提供 PGvector infrastructure。
- 保持 V4 final completion record 真实：V4 是 foundation / demo / interview-grade 阶段完成，不是生产部署完成。

候选文件：

- `README.md`
- `EXEC_PLAN_V4.md`
- `version-updates/EXEC_PLAN_V4_RAG_SPRING_AI.md`
- `docs/quality/QUALITY_SCORE.md`
- `docs/quality/VALIDATION_COMMANDS.md`
- `version-updates/V4_RELEASE_SUMMARY.md`
- `docs/demo/V4_PROJECT_HIGHLIGHTS.md`
- `docs/demo/V4_INTERVIEW_DEMO_CHECKLIST.md`
- `docs/api/OPENAPI.md`
- 任何把 PGvector / VectorStore live persistence 写成已完成能力的 V4 completion record。

验证：

- docs harness tests。
- secret / local path safety tests。
- 如果只改文档且不改 harness，通常不需要 runtime 测试。

### 阶段 1：生产配置模板

范围：

- 已新增安全的 `application-prod.example.yml` 模板。
- 所有敏感值只使用环境变量 placeholder。
- 默认 test profile 继续离线。
- 文档列出生产环境变量，但不提交真实值。
- 已新增 `docs/deploy/PRODUCTION_CONFIG_TEMPLATE.md` 和 docs harness test。

非目标：

- 不做真实生产部署。
- 不接真实 secret manager。
- 不让默认测试依赖生产配置。

验证：

- 默认 profile context 继续可加载。
- prod 模板和文档不包含真实 secret。
- Architecture 和 docs safety tests 继续通过。

### 阶段 2：可观测性加固方案

范围：

- 已新增 metrics / tracing 方向的 decision record：
  `docs/decisions/DECISION_PROJECT_REVIEW_OBSERVABILITY_HARDENING.md`。
- 已新增完成记录：
  `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE2_OBSERVABILITY_HARDENING.md`。
- 已明确当前保持 MDC-only，OpenTelemetry 是 future / opt-in path。
- 已明确 Prometheus、Grafana、collector、metrics dashboard、provider latency / cost metrics 和 external logging
  platform 仍是 future / opt-in，不是阶段 2 runtime 实现。
- Actuator 默认暴露边界继续安全，只包含 `health`。

非目标：

- 不默认暴露敏感 actuator endpoints。
- 不把外部监控平台变成默认测试依赖。
- 不实现 Micrometer instrumentation。
- 不修改 runtime 代码。

验证：

- 默认 `/actuator/health` 继续暴露。
- 敏感 actuator endpoints 继续不默认暴露。
- 默认测试不需要 Prometheus、collector 或外部网络。
- `ObservabilityHardeningDecisionDocsTest` 检查阶段 2 文档、边界和 secret safety。

### 阶段 3：API 完整性改进

#### 阶段 3.1：API Surface Audit / API Completeness Decision

状态：已完成。

范围：

- 已新增 `docs/decisions/DECISION_PROJECT_REVIEW_API_COMPLETENESS.md`。
- 已新增完成记录：
  `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_1_API_COMPLETENESS_DECISION.md`。
- 已明确当前 HTTP API 是 demo/backend API surface，不是完整生产 CRUD。
- 已记录当前 API：Ticket create/get、AgentRun create/start、ToolCallTrace read-only view、Execution Tree read-only
  view、Approval pending/get/approve/reject、health 和 OpenAPI docs。
- 已明确 `search_aftersale_policy` 是 LOW-risk read-only ToolRegistry tool，不是 public RAG HTTP endpoint。
- 已新增 docs harness test：`ApiCompletenessDecisionDocsTest`。

非目标：

- 不新增 endpoint；
- 不修改 Controller 或 DTO runtime；
- 不实现分页；
- 不实现 AgentRun get/status polling；
- 不实现异步 AgentRun；
- 不实现 SSE / WebSocket；
- 不实现 batch API；
- 不实现 production auth / RBAC。

#### 阶段 3.2：Ticket list/query pagination foundation

状态：已完成。

范围：

- 新增 `GET /api/tickets` 只读 list/query endpoint。
- 支持 `page` / `size` / `sort`。
- 支持 `status`、`userId`、`orderId`、`intentType`、`createdFrom`、`createdTo` 只读过滤。
- 补 Controller / OpenAPI / docs harness tests。
- 更新 README、OpenAPI docs、API completeness decision、整改方案和质量文档。
- 完成记录：
  `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_2_TICKET_PAGINATION.md`。

非目标：

- 不新增 AgentRun get/status endpoint。
- 不实现异步 AgentRun。
- 不实现 SSE / WebSocket。
- 不实现 batch API。
- 不实现 production auth / RBAC。
- 不新增 public RAG HTTP endpoint。
- 不修改 ToolRegistry、RAG runtime、ingestion、health、OpenAPI config、ToolCallTrace、Workspace 或 Execution Tree
  runtime。

#### 阶段 3.3：AgentRun get/status polling read model

状态：已完成。

范围：

- 新增 `GET /api/agent-runs/{runId}` 只读状态 endpoint。
- 响应包含 `runId`、`ticketId`、`status`、时间字段、final / failure summary 和 trace / execution-tree 链接。
- 补 Controller / OpenAPI / docs harness tests。
- 更新 README、OpenAPI docs、API completeness decision、整改方案、validation docs 和质量文档。
- 完成记录：
  `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_3_AGENT_RUN_STATUS_READ.md`。

非目标：

- 不实现异步 AgentRun。
- 不实现 SSE / WebSocket。
- 不实现 batch API。
- 不实现 production auth / RBAC。
- 不新增 public RAG HTTP endpoint。
- 不修改 ToolRegistry、Planner、RAG runtime、ingestion、health、OpenAPI config、ToolCallTrace、Workspace、
  Approval 或 Execution Tree runtime。

#### 阶段 3.4：Async / Streaming / Batch API Evaluation

状态：已完成。

范围：

- 新增 `docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md`。
- 新增完成记录：
  `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE3_4_ASYNC_STREAMING_BATCH_EVALUATION.md`。
- 评估 async AgentRun、status polling、SSE / WebSocket、batch API、cancel / retry 和 AgentRun list pagination。
- 明确当前同步 create/start + `GET /api/agent-runs/{runId}` status polling 是当前安全路径。
- 明确 AgentRun list pagination 是后续 read-only API 候选，优先级高于 streaming 和 batch。

非目标：

- 不实现异步 AgentRun。
- 不实现 SSE / WebSocket。
- 不实现 batch API。
- 不实现 cancel / retry API。
- 不实现 AgentRun list pagination。
- 不实现 production auth / RBAC。
- 不新增 public RAG HTTP endpoint。
- 不修改 ToolRegistry、Planner、RAG runtime、ingestion、health、OpenAPI config、ToolCallTrace、Workspace、
  Approval 或 Execution Tree runtime。

#### 阶段 3.5+：API runtime 改进候选

范围：

- 考虑 AgentRun list pagination。
- 考虑异步 AgentRun + progress model。
- 将 SSE progress / trace streaming 作为后续 opt-in API 设计。
- 在完成 auth / RBAC、idempotency、rate limit 和 partial failure model 后再考虑 batch API。

非目标：

- 不修改 ToolRegistry 执行语义。
- 不让 LLM 直接执行工具。
- 不执行真实退款、换货、支付、物流或优惠券补偿。

验证：

- Controller tests。
- OpenAPI docs tests。
- 现有 AgentRun happy path regression tests。

### 阶段 4：Spring AI 深化使用评估

状态：已完成。

范围：

- 已新增 `docs/decisions/DECISION_PROJECT_REVIEW_SPRING_AI_DEEPENING.md`。
- 已新增完成记录：
  `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE4_SPRING_AI_DEEPENING_EVALUATION.md`。
- 已评估 ChatMemory / Advisors 是否适合当前 Agent boundary。
- 已评估 Spring AI Tool Calling API 与 ToolRegistry / Approval / Trace 边界的冲突风险。
- 已评估 bulk embedding 必须保留在 `EmbeddingClient` abstraction 后。

非目标：

- 不实现 ChatMemory runtime。
- 不实现 Advisors runtime。
- 不启用 Spring AI Tool Calling API。
- 不实现 bulk embedding runtime。
- 不让 Spring AI 绕过 AgentPlan validation。
- 不让 Spring AI 直接调用项目工具。
- 不让默认测试调用真实 provider。

验证：

- `SpringAiDeepeningDecisionDocsTest`。
- Fake provider tests 继续确定性。
- Live provider tests 继续显式 opt-in。
- ArchitectureTest 继续约束 Agent / Handler / Skill 边界。

### 阶段 5：RAG 检索质量改进

状态：已完成。

范围：

- 已新增 `docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md`。
- 已新增完成记录：
  `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE5_RAG_QUALITY_EVALUATION.md`。
- 已记录当前 KEYWORD / VECTOR / HYBRID RAG baseline。
- 已记录 deterministic RAG evaluation baseline 和 no LLM-as-judge by default。
- 已评估 reranking、query rewriting、RRF / hybrid scoring、chunk window expansion 的 future / opt-in 路线。
- 已明确 V5.A.1 前 JdbcPolicyVectorRepository 未完成；当前 V5.A.1 已新增 opt-in JDBC adapter，但 live
  PGvector validation 和 Spring AI VectorStore production path 未完成。

非目标：

- 不实现 reranking runtime。
- 不实现 query rewriting runtime。
- 不实现 RRF 或 hybrid scoring runtime change。
- 不实现 chunk window expansion runtime。
- 不修改 `search_aftersale_policy` runtime。
- 不修改 retrieval algorithm。
- 不修改 RAG evaluation runner。
- 不替换默认 in-memory / fake test path。
- 不让默认测试连接 PGvector、真实 embedding provider、真实 reranker provider 或 Spring AI VectorStore。
- 不把 policy evidence 变成业务动作自动化。

验证：

- `RagQualityDecisionDocsTest`。
- RAG evaluation metrics 继续作为后续 runtime 改动 gate。
- Offline fake vector store tests 继续确定性。
- Live PGvector tests 继续 opt-in。

### 阶段 6：部署加固路线

状态：已完成。

范围：

- 新增 `docs/decisions/DECISION_PROJECT_REVIEW_DEPLOYMENT_HARDENING.md`。
- 新增 `docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md`。
- 新增完成记录：
  `version-updates/EXEC_PLAN_PROJECT_REVIEW_CORRECTION_STAGE6_DEPLOYMENT_HARDENING_ROADMAP.md`。
- 记录 Dockerfile hardening、CI/CD、secret management、database migration、PGvector deployment、
  readiness/liveness、observability、security/auth 和 release/rollback 后续路线。
- 更新 README、production config docs、整改方案、quality docs、validation docs 和 release summary。
- 新增 docs harness test：`DeploymentHardeningRoadmapDocsTest`。

非目标：

- 不把 Docker Compose 写成生产部署方案。
- 不提交真实生产凭据。
- 没有实现前，不声明生产监控已完成。
- 不新增 Dockerfile。
- 不新增 CI/CD pipeline。
- 不新增 Kubernetes / Helm。
- 不实现 secret manager。
- 不实现 production deployment、production auth/RBAC、production monitoring。
- 不实现 live PGvector validation；`JdbcPolicyVectorRepository` 已由 V5.A.1 作为 opt-in adapter 单独完成。

验证：

- Docker / compose docs harness。
- 默认 Maven 验证继续离线。
- CI 使用同一套默认 gate：
  - `mvn test`
  - `mvn checkstyle:check`
  - `mvn spotbugs:check`
  - `mvn test -Dtest=ArchitectureTest`

## 4. 建议优先级

1. 阶段 0：文档事实口径修正。
2. 阶段 1：生产配置模板。
3. 阶段 3：API 分页和 AgentRun 读取模型。
4. 阶段 2：可观测性指标决策。已完成文档决策；最小 Micrometer 集成仍是后续实现任务。
5. 阶段 4：Spring AI 深化评估。已完成文档决策；runtime 深化仍需独立任务。
6. 阶段 5：RAG 检索质量改进评估。已完成文档决策；reranking、query rewriting、RRF 和 chunk window expansion
   runtime 仍需独立任务。
7. 阶段 6：部署加固路线。已完成文档决策；Dockerfile、CI/CD、Kubernetes / Helm、secret manager、
   production auth/RBAC、production monitoring 和 live PGvector validation 仍需独立任务。
8. V5.A.1：`JdbcPolicyVectorRepository` opt-in adapter 已完成。
9. V5.A.2：schema init / version baseline `2026-06-01-001` 已完成，仅记录当前 schema 初始化口径。
10. V5.A.3：PGvector connectivity smoke 已完成，必须显式 opt-in，不得进入默认验证。
11. V5.A.4：PGvector profile / docs closure 已完成，收口记录为
    `version-updates/EXEC_PLAN_V5_A_RAG_PRODUCTION_PATH_COMPLETION.md`。
12. V5.B.1：Container + CI foundation 已完成，新增 Dockerfile、`.dockerignore`、CI quality gate 和 Docker build
    validation；不代表 production deployment。
13. V5.B.2.1：Config + Secret Boundary / Profile Matrix Plan 已完成，新增配置基线、profile matrix、secret
    boundary 和 migration follow-up 文档；不修改 runtime。
14. V5.B.2.2：Flyway migration foundation 已完成；Liquibase 未引入，Flyway 默认关闭。
15. V5.B.2.3：Profile matrix runtime validation planned。

## 5. 风险控制

- 不降低现有 ArchitectureTest、Checkstyle、SpotBugs 或 docs harness tests。
- 不把外部服务引入默认 `mvn test`。
- 不在实现前新增 production claims。
- 不让 Spring AI、Tool Calling 或 RAG 绕过 ToolRegistry、RiskPolicy、Approval、ToolCallTrace、Workspace 或 Execution Tree 边界。
- 增加任何 live integration 后，仍必须保留 fake / in-memory 默认路径。

## 6. 下一轮实现任务的 Review Packet

下一轮实现任务应明确选择一个阶段，并输出：

```text
## Review Packet

### 变更内容

### 修改文件

### 设计原因

### 保留的边界

### 验证结果

### 风险

### 后续事项

### 完成信号
TASK_COMPLETE
```
