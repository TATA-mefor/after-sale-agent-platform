# 项目整改方案：阶段 0-2 文档事实口径、生产配置模板与可观测性决策

Date: 2026-06-01

Status: Completed

## 目的

本文件用于回应项目整体审查中的 Spring Boot、Spring AI、RAG / Tool、API 和部署评价，给出中文事实核验与
阶段化整改路线。阶段 0 完成文档事实口径修正；阶段 1 补充生产配置模板和 secret placeholder 说明；
阶段 2 完成可观测性加固决策。三个阶段都不修改 runtime 代码。

## 总体结论

审查中指出的部分问题真实存在，但也有若干表述需要校准：

- 项目不是空的 Spring Boot skeleton；V4 已完成 AgentRun、ToolRegistry、Approval、ToolCallTrace、Workspace、
  Execution Tree、RAG evidence、Actuator health、OpenAPI docs、默认离线验证和 docs harness。
- 项目也不是生产完成态；production auth、production monitoring、production deployment、真实退款、换货、
  支付、物流和补偿系统接入仍是 future work。
- PGvector 当前是 profile、schema、compose、repository contract、fake / in-memory 默认路径和 opt-in boundary；
  `JdbcPolicyVectorRepository`、默认 live PGvector write/search、Spring AI `VectorStore` production path 仍未完成。
- Spring AI 当前是 adapter foundation；ChatMemory、Advisors、Tool Calling API、bulk embedding 是后续增强方向。
- RAG evidence 是政策证据，不是业务决策，也不执行任何业务动作。

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

阶段 1+ 建议：

- 先补生产配置与可观测性，再评估 ChatMemory / Advisors 是否适合本项目的 ToolRegistry 边界。
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

- 当前 API 覆盖 demo/backend surface，但缺少分页、异步 AgentRun、SSE / WebSocket 流式输出和批量操作。
- OpenAPI 已存在，但不代表新增 public RAG endpoint 或生产 API 完成。

需要修正：

- 当前 API 不应被描述为完整生产 CRUD 平台。
- `search_aftersale_policy` 是 ToolRegistry tool；没有必要为了文档新增 Controller。

阶段 1+ 建议：

- 补分页和查询条件。
- 设计异步 AgentRun 和只读执行进度查询。
- 如需流式输出，先完成事件模型和安全边界。

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

阶段 3：planned。领域模型强化。梳理 Ticket / Order 业务不变量，避免把状态规则全部留在 application service。

阶段 4：planned。API 完整性。补分页、异步 AgentRun 设计、只读进度模型和 OpenAPI 对应说明。

阶段 5：planned。RAG 检索质量。实验 reranking、query rewriting、RRF、chunk window expansion。

阶段 6：planned。Spring AI 深化。评估 ChatMemory、Advisors、Tool Calling API 与 ToolRegistry 边界的兼容性。

阶段 7：planned。部署工程化。补 Dockerfile hardening、CI/CD、secrets 管理、日志采集和部署文档。

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
mvn test -Dtest=ProjectRemediationPlanDocsTest
mvn test -Dtest=ArchitectureTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
```

## Completion Signal

TASK_COMPLETE
