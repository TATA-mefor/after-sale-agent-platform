# AfterSale-Agent 项目审查问题修正方案

状态：阶段 0-1 已完成，阶段 2+ planned

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
- HTTP API 当前没有分页、异步 AgentRun、SSE/WebSocket 流式输出或批量 API。
- 部署能力偏本地开发：没有 Kubernetes、Helm、CI/CD workflow、生产级 secret 管理、生产监控或部署加固。

### 2.2 需要修正口径的问题

- 项目不是空的 Spring Boot skeleton。Ticket、AgentRun、Approval、ToolCallTrace、Workspace、Execution Tree、RAG evaluation、Actuator health、OpenAPI docs 都已经存在。
- Ticket 不是纯贫血模型；它有状态流转和 terminal-state guard。Order 更薄，更接近只读模型。
- 当前项目没有实现手写 SQL PGvector repository。默认 vector repository 是 in-memory / fake，PGvector 当前是 profile、schema、compose、docs 和架构边界基础。
- `docker-compose-rag.yml` 提供 PGvector 本地基础设施，但当前没有在同一个 compose 文件里启动 app 服务。
- 当前 REST API 不是完整 CRUD。Ticket 有 create/get；AgentRun 有 create，以及 trace / execution-tree 只读视图；Approval 有 pending/get/approve/reject。
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
- `docs/exec-plans/active/EXEC_PLAN_V4_RAG_SPRING_AI.md`
- `docs/quality/QUALITY_SCORE.md`
- `docs/quality/VALIDATION_COMMANDS.md`
- `docs/release/V4_RELEASE_SUMMARY.md`
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

- 新增 metrics / tracing 方向的 decision record。
- 决定是否引入 Micrometer Prometheus registry。
- 决定 trace-id 传播先保持 MDC-only，还是后续接 OpenTelemetry。
- Actuator 默认暴露边界继续安全。

非目标：

- 不默认暴露敏感 actuator endpoints。
- 不把外部监控平台变成默认测试依赖。

验证：

- 默认 `/actuator/health` 继续暴露。
- 敏感 actuator endpoints 继续不默认暴露。
- 默认测试不需要 Prometheus、collector 或外部网络。

### 阶段 3：API 完整性改进

范围：

- 给适合 list 的 endpoint 增加分页。
- 按需要补一个明确的 AgentRun get endpoint。
- 考虑异步 AgentRun + status polling。
- 将 SSE progress / trace streaming 作为后续 opt-in API 评估。

非目标：

- 不修改 ToolRegistry 执行语义。
- 不让 LLM 直接执行工具。
- 不执行真实退款、换货、支付、物流或优惠券补偿。

验证：

- Controller tests。
- OpenAPI docs tests。
- 现有 AgentRun happy path regression tests。

### 阶段 4：Spring AI 深化使用

范围：

- 评估 ChatMemory / Advisor 是否适合当前 Agent boundary。
- 只在受控 adapter 中评估 Spring AI Tool Calling，不能替代 ToolRegistry。
- 如 ingestion 规模需要，在现有 EmbeddingClient abstraction 后增加 bulk embedding。

非目标：

- 不让 Spring AI 绕过 AgentPlan validation。
- 不让 Spring AI 直接调用项目工具。
- 不让默认测试调用真实 provider。

验证：

- Fake provider tests 继续确定性。
- Live provider tests 继续显式 opt-in。
- ArchitectureTest 继续约束 Agent / Handler / Skill 边界。

### 阶段 5：RAG 检索质量改进

范围：

- 增加可选 reranking abstraction。
- 只有在可离线测试时才增加 query rewrite abstraction。
- 如果 evaluation cases 证明需要，增加 RRF 或更清晰的 hybrid scoring strategy。
- 如果 evidence snippet 太窄，增加 chunk window expansion。
- JdbcPolicyVectorRepository 或 Spring AI VectorStore integration 只能作为 opt-in live path。

非目标：

- 不替换默认 in-memory/fake test path。
- 不让默认测试连接 PGvector。
- 不把 policy evidence 变成业务动作自动化。

验证：

- RAG evaluation metrics。
- Offline fake vector store tests。
- Live PGvector tests 继续 opt-in。

### 阶段 6：部署加固路线

范围：

- 优化 Dockerfile layering 和 runtime image size。
- 在适合的 compose 中增加 app healthcheck。
- 增加 CI workflow，运行默认验证命令。
- 生产部署文档作为 future work 补充。
- Helm / Kubernetes 只在生产 readiness 需求明确后再评估。

非目标：

- 不把 Docker Compose 写成生产部署方案。
- 不提交真实生产凭据。
- 没有实现前，不声明生产监控已完成。

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
4. 阶段 2：可观测性指标决策和最小 Micrometer 集成。
5. 阶段 5：由评估失败项驱动的 RAG 质量改进。
6. 阶段 6：CI 和部署加固。
7. 阶段 4：更深的 Spring AI 能力，只在不破坏当前 Agent 安全边界时推进。

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
