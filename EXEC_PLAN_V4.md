
# AfterSale-Agent V4 RAG & Spring AI Integration 执行计划

Status: active

## 1. V4 总目标

V4 的目标是在不破坏 V1/V2/V3 已建立的 Agent、ToolRegistry、Approval、Trace、Workspace、Execution Tree、Evaluation 和默认离线测试边界的前提下，将 AfterSale-Agent 升级为具备面试级说服力的 Java AI 工程项目：

```text
Spring Boot enterprise backend
+ Spring AI provider adapter
+ RAG / vectorized policy retrieval
+ PGvector-backed VectorStore profile
+ Tool / Skill capability layer
+ auditable Agent evidence chain
+ Spring Boot completeness hardening
```

V4 不继续堆叠新的售后业务动作。V4 优先解决面试官最关心的能力：

1. RAG 向量化存储和政策证据检索；
2. Spring AI 接入和 provider abstraction；
3. Tool / Skill 分层和可审计能力编排；
4. Spring Boot 工程完整性；
5. 默认离线测试、live opt-in、风险边界和可观测性。

V4 目标链路：

```text
Policy document ingestion
→ chunking
→ embedding
→ PGvector / VectorStore persistence
→ hybrid policy retrieval
→ search_aftersale_policy Tool
→ AgentSkill
→ AgentWorkspace.PolicyEvidence
→ ToolCallTrace
→ Execution Tree
→ AgentRun final summary
```

## 2. V4 核心原则

1. LLM 只负责规划，不直接执行 Tool 或 Skill；
2. RAG 只提供政策证据，不直接决定售后业务结果；
3. Tool 是原子可执行能力，必须通过 ToolRegistry 执行；
4. Skill 是复合任务能力，必须通过 SkillRegistry 调度；
5. Skill 可以组合多个 Tool，但不得绕过 ToolRegistry；
6. VectorStore 只能在 policy/rag application 或 infrastructure 边界内访问；
7. Agent、Planner、Specialist Handler、Skill 不得直接访问 Repository、VectorStore、JdbcTemplate、ChatClient 或 EmbeddingModel；
8. `search_aftersale_policy` 仍然是 LOW-risk read-only tool；
9. 每次实际 Tool 调用必须记录 ToolCallTrace；
10. Skill 执行结果必须写入 SubtaskExecutionResult、AgentWorkspace 或 Execution Tree 可读结构；
11. HIGH-risk 售后动作仍必须进入 Approval，不得自动执行；
12. 默认 `mvn test` 不得依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis、向量库或外部网络；
13. live provider、live embedding、live vector store、live MySQL / PostgreSQL 验证必须显式 opt-in；
14. 不提交真实 API Key、数据库密码、token、个人路径、raw private data 或 production secrets。

## 3. V4 不做什么

V4 明确不做：

- 不接真实退款；
- 不接真实换货；
- 不接真实优惠券补偿；
- 不接真实支付；
- 不接真实物流；
- 不接真实库存系统；
- 不接真实订单中心；
- 不做微服务拆分；
- 不做 LangChain Python sidecar 作为默认主链路；
- 不做长期用户画像；
- 不做跨会话长期记忆；
- 不做完整前端后台；
- 不把 Docker Compose 写成生产部署方案；
- 不把 live tests 加入默认 Maven test gate；
- 不让 LLM / Spring AI / RAG 绕过既有 Java 后端校验、ToolRegistry、RiskPolicy、Approval、Trace 或 Workspace。

## 4. V4.0 Pre-flight Fixes

Status: completed.

### 4.1 目标

在进入 RAG / Spring AI / Skill Layer 之前，先修复 V3 代码阅读中发现的边界一致性问题，避免 V4 功能接入后放大问题。

V4.0 只完成 pre-flight fixes，不包含 Spring AI、RAG、PGvector、VectorStore、Policy Ingestion 或 SkillRegistry 实现。

### 4.2 范围

必做：

1. Planner 可见工具集与 AgentRun 实际可执行工具集一致化；
2. AgentRun 失败时 Ticket 状态策略明确化；
3. 工程注释整理为边界说明型注释；
4. 打包边界清理：不得把 `.git/`、`target/`、`data/raw/`、`.env`、本地密钥或大体积原始数据打进提交包；
5. 更新 Harness 文档，将 V4 边界写入 AGENTS / ARCHITECTURE / TOOL / SKILL / RISK / LLM / QUALITY 文档。

### 4.3 验收标准

- Planner 只看到当前 AgentRun 编排层可执行的 Tool 或 Skill；
- 如果保留更多 Tool 对 Planner 可见，则 AgentApplicationService / Skill 必须有明确输入映射和执行支持；
- AgentRun FAILED 时 Ticket 不得无限停留在 `AGENT_RUNNING`，除非有明确文档解释和 API 可见状态；
- 注释不解释基础语法，只解释架构边界、风险边界、opt-in 边界和非显然设计选择；
- 默认验证命令全部通过。

### 4.4 完成记录

- 新增 AgentRun 级 executable tool policy，Planner 可见工具、AgentPlanValidator 校验工具和 Specialist Handler
  执行工具共享同一允许列表；
- 当前 AgentRun 可执行工具限定为 `get_order_by_id`、`search_aftersale_policy`、`add_ticket_note`；
- `create_aftersale_ticket`、`update_ticket_status`、`get_user_orders` 等已注册但当前 AgentRun 无输入映射的工具不再暴露给 Planner；
- AgentRun 失败时，如果 Ticket 仍处于 `CREATED` 或 `AGENT_RUNNING`，Ticket 会进入 `FAILED` 并保留失败摘要；
- `WAITING_HUMAN_APPROVAL` 不会被失败回写误标为失败；
- 工程注释收敛为 Agent、ToolRegistry、Approval、Trace、Workspace、LLM 规划和 profile 边界说明；
- V4.0 不改变 ToolRegistry、Approval、Trace、Workspace、Planner、Specialist Handler 的核心语义；
- 默认测试仍不依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或外部网络。

## 5. V4.1 Tool / Skill Layer Foundation

Status: completed.

### 5.1 目标

将 Tool 与 Skill 的概念从隐含实现升级为 Harness 约束和 Java 后端基础模型。

V4.1 完成 Skill foundation，不包含 Spring AI、RAG、PGvector、VectorStore、Policy Ingestion 或完整
Skill-based runtime migration。

### 5.2 必须新增或更新

```text
docs/agent/SKILL_CONTRACTS.md
docs/agent/TOOL_CONTRACTS.md
docs/agent/RISK_POLICY.md
docs/agent/LLM_PLANNER_CONTRACT.md
docs/decisions/DECISION_V4_TOOL_SKILL_LAYER.md
ARCHITECTURE.md
AGENTS.md
QUALITY_SCORE.md
```

### 5.3 Tool 定义

Tool 是 Agent 系统中的原子可执行能力。Tool 必须声明：

```text
toolName
description
inputSchema
outputSchema
riskLevel
requiresApproval
failureModes
traceFields
```

Tool 必须通过 ToolRegistry 执行。Tool 的实际执行必须进入 ToolCallTrace。

### 5.4 Skill 定义

Skill 是 Java 后端中的复合任务能力。Skill 组合 Tool、Workspace、Policy Evidence、Risk Policy 和业务规则完成一个可复用任务策略。

Skill 必须声明：

```text
skillName
description
supportedSubtaskTypes
inputSchema
outputSchema
requiredTools
optionalTools
riskLevel
requiresApprovalWhen
evidenceRequirements
workspaceReads
workspaceWrites
failureModes
```

### 5.5 Tool / Skill 关系

```text
Planner
→ AgentPlan / AgentSubtask
→ SkillRegistry
→ AgentSkill
→ ToolRegistry
→ ToolExecutor
→ ToolCallTrace
→ AgentWorkspace
→ Execution Tree
```

### 5.6 验收标准

- Skill Contract 文档完成；
- Tool Contract 明确 Tool vs Skill；
- SkillRegistry 能按 skillName 和 SubtaskType 发现已注册 Skill；
- 现有 SpecialistAgentHandler 可通过轻量 adapter 暴露为 AgentSkill；
- Skill riskLevel 不得低于 requiredTools 的最高工具风险；
- LLM Planner Contract 记录 `plannedSkills` 未来扩展边界，但 V4.1 代码暂不启用 plannedSkills；
- Risk Policy 定义 Skill risk aggregation；
- ARCHITECTURE 定义 SkillRegistry / AgentSkill 依赖边界；
- AGENTS 定义 Codex 在 V4 Tool / Skill / RAG 任务前必须阅读的文档清单。

### 5.7 完成记录

- 新增 `AgentSkill`、`SkillDefinition`、`SkillRegistry`、`SkillExecutionContext`、
  `SkillExecutionResult`、`SkillExecutionStatus` 和 `SkillExecutionException`；
- 新增 `SpecialistHandlerSkillAdapter`，将现有 Specialist Handler 以 Skill 形式暴露，同时保持原有
  ToolRegistry、ToolCallTrace、Workspace 和 Approval 行为；
- 注册 `ReturnEligibilityAssessmentSkill`、`ExchangeRecommendationSkill`、`CouponConsultationSkill`、
  `LogisticsIssueAnalysisSkill`、`GeneralAfterSaleConsultationSkill` 和 `HumanApprovalRoutingSkill`；
- 新增 `SkillRiskEvaluator`，启动时校验 Skill 风险不得低于 requiredTools 的最高风险；
- `AgentApplicationService` 当前仍使用 Specialist Handler 主执行路径，V4.1 不强制切换到 SkillRegistry；
- `plannedSkills` 未接入 AgentPlan 解析和运行时执行，保留为后续兼容扩展；
- 默认测试仍不依赖真实 LLM、API Key、PostgreSQL、PGvector、Docker、MySQL、Redis 或外部网络。

## 6. V4.2 Spring AI Adapter

Status: completed.

V4.2 completed only the Spring AI chat and embedding adapter foundation. It did not implement RAG, VectorStore,
PGvector, Policy Ingestion, Spring AI tool/function calling, or Skill runtime migration.

### 6.1 目标

在不删除现有 OpenAI / DashScope provider 和 LlmClient 边界的前提下，引入 Spring AI adapter，使项目能通过 Spring AI ChatClient 和 EmbeddingModel 接入 LLM 与 embedding provider。

### 6.2 已完成模型 / 类

```text
agent/infrastructure/springai/SpringAiLlmClient
agent/infrastructure/springai/SpringAiChatGateway
agent/infrastructure/springai/ChatClientSpringAiChatGateway
agent/infrastructure/springai/SpringAiConfiguration
common/ai/SpringAiProviderProperties
common/ai/SpringAiProviderErrorFormatter
policy/rag/infrastructure/springai/SpringAiEmbeddingClient
policy/rag/infrastructure/springai/SpringAiEmbeddingGateway
policy/rag/infrastructure/springai/EmbeddingModelSpringAiEmbeddingGateway
policy/rag/infrastructure/springai/SpringAiEmbeddingConfiguration
policy/rag/application/EmbeddingClient
policy/rag/application/FakeEmbeddingClient
```

### 6.3 配置

```yaml
agent:
  spring-ai:
    enabled: false
    chat-enabled: false
    embedding-enabled: false
  planner:
    llm:
      provider: spring-ai-chat
```

默认 profile 不创建真实 ChatClient / EmbeddingModel。真实 provider 必须显式设置 `SPRING_AI_ENABLED=true`，
并按需设置 `SPRING_AI_CHAT_ENABLED=true` 或 `SPRING_AI_EMBEDDING_ENABLED=true`。

### 6.4 已完成验收

- `agent.planner.llm.provider=spring-ai-chat` 可被配置；
- Spring AI ChatClient adapter 输出仍进入 AgentPlanParser 和 AgentPlanValidator；
- Spring AI EmbeddingModel adapter 只用于 embedding，不用于工具执行；
- 默认测试使用 fake embedding client，不调用真实 Spring AI provider；
- provider error 不输出 API Key、full prompt、database password 或 sensitive credentials；
- live smoke test 显式 opt-in。

## 7. V4.3 Vector Store / PGvector

Status: completed for V4.3.1 through V4.3.4. V4.3.1 PostgreSQL / PGvector dependency and profile boundary, V4.3.2 vector schema /
repository contract, V4.3.3 fake vector store / default offline vector tests, and V4.3.4 Docker Compose /
opt-in integration docs are completed. V4.4.1 Policy Ingestion domain / status / repository foundation and V4.4.2
chunking / checksum dedup service are completed. V4.4.3 fake-provider embedding pipeline and V4.4.4 ingestion docs /
completion record are completed. Hybrid RAG runtime remains planned.

### 7.1 目标

引入显式 opt-in 的 PostgreSQL + PGvector profile，用于持久化政策 chunk embedding，并支持向量相似度检索。

### 7.2 推荐 profile

```text
default          -> in-memory repositories, fake embedding, no vector db
mysql            -> V3 MySQL persistence
rag-postgres     -> PostgreSQL + PGvector for policy RAG
spring-ai-live   -> real Spring AI provider, explicit opt-in
```

### 7.3 V4.3.1 已完成边界

V4.3.1 只完成 PostgreSQL / PGvector dependency and profile boundary:

- 新增 PostgreSQL JDBC runtime dependency，保留 MySQL profile 依赖和语义；
- 默认 `application.yml` 增加 `agent.rag.vector-store.pgvector.*` 配置，默认 disabled；
- 新增 `rag-postgres` profile 配置，显式 opt-in 才开启 PGvector boundary；
- 新增 PGvector properties / profile guard，用于校验 URL、username、password、schema、dimension；
- 配置错误只输出缺失项和 provider boundary，不输出数据库密码；
- profile guard 不创建 PostgreSQL `DataSource`、`JdbcTemplate`、Spring AI `VectorStore`、schema、repository 或连接；
- 默认测试和 MySQL profile 均不依赖 PostgreSQL、PGvector、Docker、MySQL live service、Redis、真实 LLM 或外部网络；
- ArchitectureTest 增加 Agent / Handler / Skill 不直接依赖 PGvector、VectorStore、`DataSource` 或 `JdbcTemplate` 的边界。

V4.3.1 不包含 schema、repository、VectorStore search、Policy Ingestion、RAG、Docker Compose PGvector service 或
`search_aftersale_policy` 行为变更。

### 7.4 V4.3.2 已完成边界

V4.3.2 只完成 vector schema and repository contract:

- 新增 `schema-rag-postgres.sql`，定义 `policy_documents`、`policy_chunks`、`policy_embeddings` 和 PGvector
  extension / constraints / indexes；
- schema 文件只用于后续 `rag-postgres` opt-in 路径，默认 profile 不自动加载；
- 新增纯 domain 模型 `PolicyDocument`、`PolicyChunk`、`PolicyEmbedding`、`VectorSearchQuery`、
  `VectorSearchResult`、`VectorSearchMatch` 和 `PolicyDocumentSourceType`；
- 新增 `PolicyVectorRepository` contract，只表达 save / find / search 边界，不提供 JDBC implementation；
- schema harness、domain model、repository contract tests 均不连接 PostgreSQL、PGvector、Docker 或外部网络；
- ArchitectureTest 增加 `policy.rag.domain` 纯 domain 规则，以及 Agent / Handler / Skill 不直接依赖
  `PolicyVectorRepository` 的边界。

V4.3.2 不包含 JDBC repository、Spring AI `VectorStore` search、EmbeddingClient 调用、Policy Ingestion、RAG /
HYBRID retrieval、Docker Compose PGvector service 或 `search_aftersale_policy` 行为变更。

### 7.5 V4.3.3 已完成边界

V4.3.3 只完成 fake vector store / default offline vector tests:

- 新增 `CosineSimilarityCalculator`，用于 deterministic cosine similarity evidence score；
- 新增 `InMemoryPolicyVectorRepository`，实现 `PolicyVectorRepository` 的 save / find / search contract；
- fake repository 支持 `topK`、`minScore`、category、productType、effectiveAt 和 embeddingModel filter；
- fake repository 拒绝重复 document / chunk / embedding，并要求 chunk / embedding 的父对象先保存；
- fake provider 可通过 `agent.rag.vector-store.provider=fake` 显式启用，不创建 PostgreSQL `DataSource`、
  `JdbcTemplate`、Spring AI `VectorStore` 或真实 embedding provider；
- 默认离线测试覆盖 similarity、repository contract、search ranking、filters、empty result、duplicate behavior
  和 fake provider bean boundary；
- ArchitectureTest 增加 fake vector infrastructure 不依赖 JDBC、Spring AI、业务 Repository、Tool、Handler 或
  Skill 的边界。

V4.3.3 不包含 JDBC repository、PGvector live search、Spring AI `VectorStore` search、EmbeddingClient 调用、
Policy Ingestion、RAG / HYBRID retrieval、Docker Compose PGvector service 或 `search_aftersale_policy` 行为变更。

### 7.6 V4.3.4 已完成边界

V4.3.4 只完成 Docker Compose / opt-in PGvector integration docs:

- 新增独立 `docker-compose-rag.yml`，只启动 local development PGvector PostgreSQL 服务，不让默认
  `docker-compose.yml` app + MySQL 路径依赖 PGvector；
- 新增 `.env.rag.example`，使用 placeholder local development credentials，并记录 `rag-postgres` / PGvector
  profile 环境变量；
- 新增 `docs/demo/V4_PGVECTOR_LOCAL_SETUP.md`，说明启动、停止、清理 volume、health check、schema 初始化和
  常见问题；
- `schema-rag-postgres.sql` 仅通过 opt-in compose 挂载到 initdb 路径，新 volume 初始化时才执行；
- 新增 compose/docs harness tests，验证 compose 存在、secret safety、默认 compose 未被 PGvector 污染、文档明确
  no JDBC repository / no Policy Ingestion / no HYBRID retrieval / `search_aftersale_policy` not wired；
- 默认测试仍不启动 Docker，不连接 PostgreSQL、PGvector、MySQL、Redis、真实 LLM、embedding provider 或外部网络。

V4.3.4 不包含 `JdbcPolicyVectorRepository`、PGvector live search、Spring AI `VectorStore` search、
EmbeddingClient 调用、Policy Ingestion、RAG / HYBRID retrieval、app 默认连接 PGvector 或
`search_aftersale_policy` 行为变更。PGvector compose 是 local development only，不是 production deployment。

### 7.7 后续拆分

```text
V4.3.3 -> fake vector store / default offline vector tests (completed)
V4.3.4 -> Docker Compose / opt-in integration docs (completed)
V4.4.1 -> Policy Ingestion domain / status / repository foundation (completed)
V4.4.2 -> chunking and checksum dedup (completed)
V4.4.3 -> embedding pipeline with fake provider (completed)
V4.4.4 -> ingestion docs / completion record (completed)
V4.5   -> Hybrid RAG Policy Search Tool
```

### 7.8 Schema boundary

```text
policy_documents
policy_chunks
policy_embeddings
```

字段建议：

```text
policy_documents:
- document_id
- title
- category
- product_type
- version
- source_type
- effective_from
- effective_to
- checksum
- created_at

policy_chunks:
- chunk_id
- document_id
- chunk_index
- content
- token_estimate
- metadata_json
- created_at

policy_embeddings:
- embedding_id
- chunk_id
- embedding_model
- embedding_dimension
- embedding
- created_at
```

`policy_ingestion_runs` 不在 V4.3.2 schema 中实现。V4.4.1 已定义 ingestion domain/status/repository contract，
但未新增数据库 schema、JDBC repository 或 ingestion runtime。当前 vector schema 使用
`vector(1536)` 作为默认 OpenAI-compatible embedding dimension，占位维度来自配置
`AFTERSALE_EMBEDDING_DIMENSION`；Java contract 不硬编码单一维度。

### 7.9 验收标准

- PGvector profile 是显式 opt-in；
- 默认 `mvn test` 不需要 PostgreSQL、PGvector、Docker 或外部网络；
- 本地 Docker Compose 可选支持 PostgreSQL + PGvector；
- schema / migration 有 harness tests；
- vector repository 有 fake implementation 支持默认测试；
- Agent / Handler / Skill 不直接访问 VectorStore。

## 8. V4.4 Policy Ingestion

Status: completed. V4.4.1 Policy document / chunk domain and ingestion run model, V4.4.2 chunking / checksum dedup
service, V4.4.3 fake-provider embedding pipeline, and V4.4.4 ingestion docs / completion record are completed.
Ingestion API and RAG / HYBRID retrieval remain planned.

### 8.1 目标

提供可复现、可诊断、可测试的政策文档入库流程。

### 8.1.1 V4.4.1 已完成边界

V4.4.1 只完成 Policy Ingestion domain model / status model / repository contract / in-memory persistence foundation:

- 新增 `PolicyIngestionRun`、`PolicyIngestionStatus`、`PolicyIngestionSource`、`PolicyIngestionDocument`、
  `PolicyIngestionChunk`、`PolicyIngestionError` 等纯 domain 模型；
- 新增 `PolicyIngestionStateMachine`，定义 CREATED / RUNNING / CHUNKED / EMBEDDING / COMPLETED / FAILED /
  PARTIALLY_FAILED / CANCELLED 的合法状态流转，终态不可再次变更；
- 新增 `PolicyIngestionRepository` contract，独立于 `PolicyVectorRepository`，只表达 ingestion run / document /
  chunk / error 持久化边界；
- 新增 `InMemoryPolicyIngestionRepository`，用于默认离线测试，不连接 PostgreSQL、PGvector、MySQL、Docker 或
  外部网络；
- duplicate run/document/chunk/error 行为明确：`saveRun` 拒绝重复 runId，`updateRun` 覆盖已存在 run，
  document/chunk/error 重复 ID 被拒绝；
- error message / sanitized details 会截断和脱敏，避免输出 API Key、database password、token、prompt 或本地路径；
- ArchitectureTest 增加 ingestion domain / memory infrastructure / Agent runtime 边界。

V4.4.1 不包含 chunking service、checksum dedup service、EmbeddingClient 调用、PolicyVectorRepository 写入、
JdbcPolicyIngestionRepository、JdbcPolicyVectorRepository、Policy Ingestion API、RAG / HYBRID retrieval 或
`search_aftersale_policy` 行为变更。Policy Ingestion 是 admin / pipeline capability，不是 Agent 自动工具。

### 8.1.2 V4.4.2 已完成边界

V4.4.2 只完成 chunking / checksum / dedup service:

- 新增 `PolicyChunkingOptions`、`PolicyChunkingStrategy`、`PolicyChunkingResult` 和 `PolicyChunkingService`，
  使用 deterministic character-window chunking，不依赖外部 tokenizer、LLM 或 embedding provider；
- chunk index 从 0 开始，`tokenEstimate` 使用 `ceil(chars / tokenEstimateDivisor)` 的简单确定性估算；
- chunking 支持 overlap、段落边界优先和 `maxChunksPerDocument` 上限，超过上限时失败且不输出完整 raw text；
- 新增 `PolicyContentChecksumService`、`PolicyChecksum` 和 `ChecksumAlgorithm`，用 Java 标准库 SHA-256
  计算 document / chunk checksum，并明确执行 line-ending normalization 和 trim；
- 新增 `PolicyIngestionDedupService`、`PolicyDedupDecision` 和 `PolicyDedupDecisionType`，基于
  `PolicyIngestionRepository` 的 checksum 查询判断 `NEW_CONTENT`、`DUPLICATE_DOCUMENT` 或
  `DUPLICATE_CHUNK`；
- `PolicyIngestionRepository` 增加 checksum 查询方法，`InMemoryPolicyIngestionRepository` 同步实现；
- 单元测试覆盖 blank rawText、短/长文本 chunking、overlap、paragraph boundary、max chunk overflow、
  checksum determinism、dedup decisions、repository checksum queries 和 ArchitectureTest 边界。

V4.4.2 不包含 EmbeddingClient 调用、Spring AI 调用、PolicyVectorRepository 写入、JdbcPolicyIngestionRepository、
JdbcPolicyVectorRepository、Admin Controller、ingestion tool、RAG / HYBRID retrieval 或 `search_aftersale_policy`
行为变更。Policy Ingestion 仍是 admin / pipeline capability，不是 Agent 自动工具。

### 8.1.3 V4.4.3 已完成边界

V4.4.3 只完成 fake-provider embedding pipeline:

- 新增 `PolicyEmbeddingPipelineOptions`、`PolicyEmbeddingPipelineResult`、`PolicyEmbeddingPipelineFailure` 和
  `PolicyEmbeddingPipelineService`；
- pipeline 读取 `PolicyIngestionRepository` 中的 run / document / chunk，使用 `EmbeddingClient` abstraction
  调用 fake provider 测试路径，并写入 `PolicyVectorRepository` contract；
- 默认测试使用 `FakeEmbeddingClient` 和 `InMemoryPolicyVectorRepository`，验证 document / chunk / embedding
  保存后可直接通过 repository search 找到 evidence chunk；
- ingestion document / chunk 到 vector document / chunk 的 ID 映射保持 deterministic，embedding ID 使用
  chunkId + model hash 派生，测试不依赖随机 ID；
- pipeline 支持 expectedDimension、duplicate embedding skip/fail、maxChunksPerRun、partial failure、all failure 和
  sanitized failure 结果；
- run 状态从 CHUNKED 进入 EMBEDDING，并根据结果进入 COMPLETED / PARTIALLY_FAILED / FAILED；
- ArchitectureTest 允许 ingestion application 依赖 `EmbeddingClient` abstraction 和 `PolicyVectorRepository`
  contract，但继续禁止 Spring AI adapter、Spring AI VectorStore、JDBC、DataSource、PGvector infrastructure、
  vector memory infrastructure、业务 repository、Tool、Handler 和 Skill 依赖。

V4.4.3 不包含真实 Spring AI embedding call、`SpringAiEmbeddingClient` default tests、Spring AI `VectorStore`、
PGvector / JDBC repository、`JdbcPolicyVectorRepository`、`JdbcPolicyIngestionRepository`、Admin Controller、
ingestion tool、RAG / HYBRID retrieval 或 `search_aftersale_policy` 行为变更。Policy Ingestion 仍是 admin /
pipeline capability，不是 Agent 自动工具。

### 8.1.4 V4.4.4 已完成边界

V4.4.4 只完成 ingestion docs / V4.4 completion record:

- 新增 `docs/demo/V4_POLICY_INGESTION_PIPELINE.md`，说明 ingestion 目的、当前能力、当前不包含、pipeline flow、
  default offline example、future real path、failure handling 和 security / safety；
- 新增 `docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_INGESTION_FOUNDATION.md`，作为 V4.4 总收口记录；
- README、RAG policy retrieval contract、RAG vector store decision、quality summary 和 V4 主计划均标记
  V4.4.1 / V4.4.2 / V4.4.3 / V4.4.4 completed；
- 新增 docs harness tests，验证 ingestion 文档存在、边界措辞完整、默认测试隔离说明完整、无真实 secret 或本地绝对路径。

V4.4.4 不新增 ingestion 运行时代码，不修改 chunking/checksum/embedding pipeline 行为，不调用
EmbeddingClient，不调用 PolicyVectorRepository，不连接 PostgreSQL / PGvector，不新增 Admin Controller，不新增
ingestion tool，不接入 ToolRegistry，不实现 RAG / HYBRID retrieval，也不修改 `search_aftersale_policy` 或
AgentRun 主链路。

### 8.1.5 V4.4 总收口

V4.4 已完成 policy ingestion foundation:

- V4.4.1: domain / status / repository foundation；
- V4.4.2: deterministic chunking / checksum / dedup；
- V4.4.3: fake-provider embedding pipeline；
- V4.4.4: ingestion docs / V4.4 completion record。

V4.4 未实现 Admin API、ToolRegistry ingestion tool、PGvector JDBC write、real provider default path、RAG runtime
或 HYBRID retrieval。V4.5 下一步是 Hybrid RAG Policy Search Tool，将 `search_aftersale_policy` 接入受控的
HYBRID retrieval。

### 8.2 Ingestion Flow

```text
Markdown / JSON / seed policy
→ DocumentReader
→ metadata extraction
→ checksum de-duplication
→ chunk splitter
→ embedding generation
→ vector store write
→ ingestion run status
```

### 8.3 预期类

```text
PolicyIngestionApplicationService
PolicyDocumentReader
PolicyChunkingService
PolicyEmbeddingService
PolicyIngestionRepository
PolicyVectorRepository
```

### 8.4 验收标准

- 重复导入同一文档不会重复生成 chunk；
- chunk 有 documentId、chunkIndex、category、effective date 和 token estimate；
- embedding 失败有明确状态；
- ingestion run 可查询；
- 默认测试使用 FakeEmbeddingClient 和 FakeVectorRepository；
- raw private data 不入仓；
- admin ingestion API 如暴露，必须受 Security / profile / role 控制。

## 9. V4.5 Hybrid RAG Policy Search Tool

### 9.1 目标

将 `search_aftersale_policy` 从 deterministic keyword retrieval 升级为可配置的 hybrid retrieval，同时保持 LOW-risk、read-only、auditable tool 边界。

### 9.2 Tool Input

```json
{
  "query": "质量问题 退货 退款",
  "categories": ["RETURN", "REFUND"],
  "productType": "electronics",
  "retrievalMode": "HYBRID",
  "topK": 5,
  "minScore": 0.65
}
```

### 9.3 Tool Output

```json
{
  "results": [
    {
      "chunkId": "chunk-001",
      "documentId": "policy-001",
      "documentTitle": "售后退货退款政策",
      "category": "RETURN",
      "productType": "electronics",
      "snippet": "质量问题在签收后七天内可申请退货退款...",
      "score": 0.82,
      "retrievalMode": "HYBRID",
      "effectiveFrom": "2026-01-01",
      "effectiveTo": null
    }
  ],
  "message": "Found 1 policy evidence chunk.",
  "fallbackUsed": false
}
```

### 9.4 验收标准

- 支持 KEYWORD / VECTOR / HYBRID；
- unsupported query 返回结构化空结果，不编造依据；
- ToolCallTrace outputJson 包含 chunkId、documentId、score、retrievalMode；
- AgentWorkspace.PolicyEvidence 保存 RAG evidence；
- Execution Tree 可展示 policy evidence；
- 默认测试不依赖真实 embedding provider 或 vector store；
- RAG evidence 不得直接声称退款、换货或补偿已完成。

## 10. V4.6 Skill Layer Integration

### 10.1 目标

在现有 SpecialistAgentHandler 基础上引入 Skill 抽象，不一次性大规模重写已有 Handler。

### 10.2 迁移策略

```text
SpecialistAgentHandler
→ AgentSkill adapter
→ SkillRegistry
→ gradual replacement / coexistence
```

### 10.3 预期 Skill

```text
ReturnEligibilityAssessmentSkill
ExchangeRecommendationSkill
CouponConsultationSkill
LogisticsIssueAnalysisSkill
GeneralAfterSaleConsultationSkill
HumanApprovalRoutingSkill
RagPolicyEvidenceSkill
```

### 10.4 验收标准

- SkillRegistry 能按 skillName 或 subtaskType 找到唯一 Skill；
- Skill 内部调用 Tool 必须通过 ToolRegistry；
- Skill 不直接访问 Repository / VectorStore / Spring AI clients；
- SkillExecutionResult 结构化表达 status、summary、evidence、toolCalls、riskFlags、approvalRequirement；
- Execution Tree 能展示 Skill node；
- 现有 V2/V3 demo 不退化。

## 11. V4.7 Execution Tree / Evaluation / Demo

### 11.1 Execution Tree

新增 Skill node 与 RAG evidence node：

```text
AgentRun
├── Subtask: RETURN
│   ├── Skill: ReturnEligibilityAssessmentSkill
│   │   ├── ToolCall: get_order_by_id
│   │   ├── ToolCall: search_aftersale_policy
│   │   └── ToolCall: add_ticket_note
│   └── PolicyEvidence
│       ├── chunkId
│       ├── score
│       └── retrievalMode
└── ApprovalRequest optional
```

### 11.2 Evaluation

新增指标：

```text
policyEvidenceRecallAccuracy
ragCitationCompleteness
unsupportedQueryNoFabricationRate
skillSelectionAccuracy
skillExecutionBoundaryPassRate
```

### 11.3 Demo

新增 `docs/demo/V4_RAG_SKILL_DEMO_SCRIPT.md`，演示：

1. 导入售后政策文档；
2. chunk + embedding + vector write；
3. 创建售后工单；
4. AgentRun 调用 Skill；
5. Skill 通过 ToolRegistry 调 RAG search tool；
6. ToolCallTrace 显示 RAG evidence；
7. Execution Tree 显示 skill node、tool node、evidence node；
8. final summary 引用政策证据，但不执行真实高风险动作。

## 12. V4.8 Spring Boot Completeness

### 12.1 目标

围绕 RAG 和 Skill 能力补齐 Spring Boot 企业级工程完整性。

### 12.2 必做方向

```text
ConfigurationProperties
Flyway or Liquibase migration
Actuator HealthIndicator
OpenAPI / springdoc
Minimal Security for admin / approval / ingestion APIs
Opt-in integration tests
Docker Compose local rag profile
```

### 12.3 验收标准

- 所有外部 provider / datasource / vector config 通过 typed properties 管理；
- migration 不破坏默认 in-memory profile；
- health 能显示 vector store / embedding provider / ingestion status；
- OpenAPI 能覆盖 Ticket、AgentRun、Approval、Execution Tree、Policy Ingestion、RAG Search；
- admin ingestion API 有最小权限边界；
- integration tests 显式 opt-in；
- default test gate 不依赖外部服务。

## 13. 验证命令

每个 V4 阶段必须至少运行：

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

如新增 profile / live / integration test，必须使用显式 opt-in 命令，例如：

```bash
mvn test -Dtest=RagVectorStoreLiveTest -Dlive.rag=true
mvn test -Dtest=SpringAiEmbeddingClientLiveSmokeTest -Dlive.spring-ai=true -Dlive.embedding=true
mvn test -Dtest=V4RealAgentRagLiveTest -Dlive.llm=true -Dlive.rag=true
```

默认命令不得触发真实外部依赖。

## 14. Review Packet 要求

每个 V4 子阶段完成后必须输出 Review Packet，至少包括：

```text
changed files
behavior changes
boundary preservation
risk assessment
validation commands
known limitations
follow-ups
completion signal
```

完成文件应放入：

```text
docs/exec-plans/completed/
```

## 15. Completion Signal

```text
TASK_COMPLETE
```
