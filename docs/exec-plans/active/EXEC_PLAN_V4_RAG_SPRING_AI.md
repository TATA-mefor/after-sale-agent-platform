
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

### 6.1 目标

在不删除现有 OpenAI / DashScope provider 和 LlmClient 边界的前提下，引入 Spring AI adapter，使项目能通过 Spring AI ChatClient 和 EmbeddingModel 接入 LLM 与 embedding provider。

### 6.2 预期模型 / 类

```text
agent/infrastructure/springai/SpringAiChatClientAdapter
policy/rag/infrastructure/springai/SpringAiEmbeddingClientAdapter
common/ai/SpringAiProviderProperties
common/ai/SpringAiConfiguration
policy/rag/application/EmbeddingClient
policy/rag/application/FakeEmbeddingClient
```

### 6.3 配置目标

```yaml
agent:
  planner:
    llm:
      provider: spring-ai

rag:
  embedding:
    provider: spring-ai
```

默认 profile 不创建真实 ChatClient / EmbeddingModel。live profile 或显式 opt-in profile 才允许真实 provider。

### 6.4 验收标准

- `agent.planner.llm.provider=spring-ai` 可被配置；
- Spring AI ChatClient adapter 输出仍进入 AgentPlanParser 和 AgentPlanValidator；
- Spring AI EmbeddingModel adapter 只用于 embedding，不用于工具执行；
- 默认测试使用 fake client；
- provider error 不输出 API Key、full prompt、database password 或 sensitive credentials；
- live smoke test 显式 opt-in。

## 7. V4.3 Vector Store / PGvector

### 7.1 目标

引入显式 opt-in 的 PostgreSQL + PGvector profile，用于持久化政策 chunk embedding，并支持向量相似度检索。

### 7.2 推荐 profile

```text
default          -> in-memory repositories, fake embedding, no vector db
mysql            -> V3 MySQL persistence
rag-postgres     -> PostgreSQL + PGvector for policy RAG
spring-ai-live   -> real Spring AI provider, explicit opt-in
```

### 7.3 预期 schema

```text
policy_documents
policy_chunks
policy_embeddings
policy_ingestion_runs
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

policy_ingestion_runs:
- run_id
- source_type
- status
- document_count
- chunk_count
- embedded_count
- failed_count
- error_message
- started_at
- finished_at
```

### 7.4 验收标准

- PGvector profile 是显式 opt-in；
- 默认 `mvn test` 不需要 PostgreSQL、PGvector、Docker 或外部网络；
- 本地 Docker Compose 可选支持 PostgreSQL + PGvector；
- schema / migration 有 harness tests；
- vector repository 有 fake implementation 支持默认测试；
- Agent / Handler / Skill 不直接访问 VectorStore。

## 8. V4.4 Policy Ingestion

### 8.1 目标

提供可复现、可诊断、可测试的政策文档入库流程。

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
PolicyIngestionRunRepository
PolicyDocumentRepository
PolicyChunkRepository
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
mvn test -Dtest=SpringAiEmbeddingLiveSmokeTest -Dlive.embedding=true
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
