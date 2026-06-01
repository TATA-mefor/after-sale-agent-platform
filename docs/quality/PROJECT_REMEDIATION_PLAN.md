# 项目整改方案：阶段 0 文档事实口径修正

Date: 2026-06-01

Status: Completed

## 目的

本文件用于回应项目整体审查中的 Spring Boot、Spring AI、RAG / Tool、API 和部署评价，给出中文事实核验与
阶段化整改路线。阶段 0 只做文档事实口径修正，不修改 runtime 代码。

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

- 缺少 `application-prod.yml` 或生产配置模板。
- 生产级 metrics、Prometheus registry、distributed tracing、cross-service trace-id propagation 还未完成。
- 部分核心模型仍偏薄，业务不变量可以继续向 domain 层下沉。

需要修正：

- “所有配置堆在一个 yaml”不准确；当前已有 default、mysql、rag-postgres profile 分离。
- “工程 skeleton”偏重；当前已经具备可运行、可测试、可审计的模块化单体基础。

阶段 1+ 建议：

- 增加生产配置模板和配置说明。
- 增加 metrics / tracing 决策文档，再决定是否接 Prometheus 或 OpenTelemetry。
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

阶段 1：生产配置模板。补 `application-prod.yml` 示例、环境变量表和 secret safety 说明。

阶段 2：可观测性决策。补 metrics / Prometheus / tracing ADR，决定默认不启用还是 opt-in。

阶段 3：领域模型强化。梳理 Ticket / Order 业务不变量，避免把状态规则全部留在 application service。

阶段 4：API 完整性。补分页、异步 AgentRun 设计、只读进度模型和 OpenAPI 对应说明。

阶段 5：RAG 检索质量。实验 reranking、query rewriting、RRF、chunk window expansion。

阶段 6：Spring AI 深化。评估 ChatMemory、Advisors、Tool Calling API 与 ToolRegistry 边界的兼容性。

阶段 7：部署工程化。补 Dockerfile hardening、CI/CD、secrets 管理、日志采集和部署文档。

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

## 验证命令

```bash
mvn test -Dtest=ProjectRemediationPlanDocsTest
mvn test -Dtest=ArchitectureTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
```

## Completion Signal

TASK_COMPLETE
