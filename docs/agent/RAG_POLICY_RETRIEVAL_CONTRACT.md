
# RAG Policy Retrieval Contract

## 1. 目标

本文件定义 V4 `search_aftersale_policy` RAG 政策检索契约。V4 将现有 deterministic keyword policy retrieval 升级为支持 KEYWORD / VECTOR / HYBRID 的结构化政策证据检索能力。

核心原则：

```text
RAG retrieves evidence.
RAG does not execute business actions.
```

RAG 只能提供政策证据，不能替代 AgentPlanValidator、RiskPolicy、Approval、ToolRegistry、AgentWorkspace 或业务服务。

## 2. 检索边界

V4.2 status: only the embedding provider adapter boundary exists. `EmbeddingClient`, `FakeEmbeddingClient`, and
`SpringAiEmbeddingClient` prepare for later vector retrieval, but V4.2 does not create VectorStore, PGvector schema,
policy chunk ingestion, similarity search, or HYBRID retrieval runtime.

V4.3.1 status: only the PostgreSQL / PGvector dependency and profile boundary exists. `rag-postgres` is explicit
opt-in, PGvector settings are default-off, and the profile guard validates configuration without creating a
PostgreSQL `DataSource`, `JdbcTemplate`, Spring AI `VectorStore`, policy schema, repository, or database connection.
`search_aftersale_policy` behavior remains unchanged in V4.3.1.

V4.3.2 status: only the vector schema and repository contract exist. `schema-rag-postgres.sql` defines
`policy_documents`, `policy_chunks`, and `policy_embeddings` for the future opt-in PGvector path, and
`PolicyVectorRepository` defines a pure domain contract for saving documents, chunks, embeddings, and searching vector
matches. V4.3.2 does not add a JDBC repository, fake vector store, Spring AI `VectorStore`, embedding calls, ingestion,
RAG runtime, HYBRID retrieval, or any `search_aftersale_policy` behavior change.

V4.3.3 status: only the fake vector store and default offline vector tests exist. `InMemoryPolicyVectorRepository`
implements the `PolicyVectorRepository` contract with deterministic cosine similarity, repository filtering, ranking,
empty-result behavior, and duplicate rejection. V4.3.3 does not add a JDBC repository, PGvector live search, Spring AI
`VectorStore`, embedding calls, ingestion, RAG runtime, HYBRID retrieval, or any `search_aftersale_policy` behavior
change.

V4.3.4 status: only the Docker Compose / opt-in PGvector integration docs exist. `docker-compose-rag.yml`,
`.env.rag.example`, and `docs/demo/V4_PGVECTOR_LOCAL_SETUP.md` provide a local development PGvector startup and schema
initialization path. V4.3.4 does not add a JDBC repository, PGvector live search, Spring AI `VectorStore`, embedding
calls, ingestion, RAG runtime, HYBRID retrieval, or any `search_aftersale_policy` behavior change.

V4.4.1 status: only the Policy Ingestion domain / status / repository foundation exists. The project defines
ingestion runs, sources, ingestion documents, ingestion chunks, errors, legal status transitions, a repository
contract, and an in-memory repository for offline tests. V4.4.1 does not add chunking, checksum deduplication,
EmbeddingClient calls, PolicyVectorRepository writes, JDBC ingestion persistence, ingestion API/tool, RAG runtime,
HYBRID retrieval, or any `search_aftersale_policy` behavior change.

V4.4.2 status: only the chunking / checksum / dedup service boundary exists. The project defines deterministic
chunking options/service, SHA-256 content checksum calculation, and checksum-based document/chunk duplicate decisions
against the ingestion repository. V4.4.2 does not call EmbeddingClient, call Spring AI, write PolicyVectorRepository,
add JDBC ingestion persistence, add ingestion API/tool, implement RAG runtime, implement HYBRID retrieval, or change
`search_aftersale_policy` behavior.

V4.4.3 status: only the fake-provider embedding pipeline boundary exists. The project defines
`PolicyEmbeddingPipelineService`, options, result, and failure models. The pipeline may use the `EmbeddingClient`
abstraction with `FakeEmbeddingClient` in default offline tests and may write `PolicyDocument`, `PolicyChunk`, and
`PolicyEmbedding` through the `PolicyVectorRepository` contract with `InMemoryPolicyVectorRepository`. V4.4.3 does
not call the real Spring AI embedding adapter in default tests, call Spring AI `VectorStore`, connect PostgreSQL /
PGvector, add JDBC repositories, add ingestion API/tool, implement RAG runtime, implement HYBRID retrieval, or change
`search_aftersale_policy` behavior.

V4.4.4 status: only ingestion documentation and the V4.4 completion record were added. V4.4.4 documents the ingestion
pipeline foundation, default offline path, failure handling, safety boundary, and future real-provider path. It does
not add runtime ingestion code, Admin Controller, `ingest_policy_document` tool, ToolRegistry wiring, real Spring AI
embedding default path, JDBC repositories, PGvector live writes, RAG runtime, HYBRID retrieval, or any
`search_aftersale_policy` behavior change.

V4.5.1 status: schema preparation only. The project now defines `RetrievalMode`, `RagPolicySearchQuery`,
`RagPolicyEvidenceSource`, `RagPolicyEvidence`, `RagPolicySearchResult`, and keyword/vector result mappers. V4.5.1
does not change `search_aftersale_policy` runtime, implement keyword + vector merge service, call EmbeddingClient,
call PolicyVectorRepository.search, connect PostgreSQL / PGvector, call Spring AI VectorStore, modify AgentRun,
modify ToolCallTrace output, or modify AgentWorkspace writes. V4.5.2 handles keyword + vector merge service, V4.5.3
handles `search_aftersale_policy` HYBRID mode runtime wiring, and V4.5.4 handles ToolCallTrace / Workspace evidence
wiring.

V4.5.2 status: only keyword + vector merge service exists. The project now defines `RagPolicyEvidenceMergeOptions`
and `RagPolicyEvidenceMergeService` for deterministic score merge, dedup, topK, minScore, and fallback behavior over
already supplied KEYWORD / VECTOR RAG evidence. V4.5.2 does not change `search_aftersale_policy` runtime, call
`EmbeddingClient`, call `PolicyVectorRepository.search`, access keyword or vector repositories, connect PostgreSQL /
PGvector, call Spring AI VectorStore, modify AgentRun, modify ToolCallTrace output, or modify AgentWorkspace writes.
V4.5.3 handles `search_aftersale_policy` HYBRID mode runtime wiring.

V4.5.3 status: `search_aftersale_policy` now supports KEYWORD / VECTOR / HYBRID runtime modes. Old calls without
`retrievalMode` remain KEYWORD. VECTOR mode uses the `EmbeddingClient` abstraction and `PolicyVectorRepository.search`
contract when both are available; HYBRID mode combines keyword and vector evidence through
`RagPolicyEvidenceMergeService`. Default tests use `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository` and do
not connect PostgreSQL / PGvector, call real Spring AI EmbeddingModel, call Spring AI VectorStore, require API keys, or
use external network. V4.5.3 does not change ToolCallTrace schema, AgentWorkspace evidence writes, AgentRun, Skill
runtime, ToolRegistry semantics, or Execution Tree. V4.5.4 now completes ToolCallTrace / Workspace evidence
visibility.

V4.5.4 status: ToolCallTrace / Workspace evidence wiring completed. `search_aftersale_policy` output JSON is the
ToolCallTrace audit surface for RAG evidence, AgentWorkspace stores single-run policy evidence summaries, AgentRun
final summary includes concise evidence references, and Execution Tree read-only output can show policy evidence
summaries associated with subtask/tool call metadata when available. V4.5.4 does not change KEYWORD / VECTOR / HYBRID
retrieval algorithms, does not change ToolCallTrace schema, does not connect PGvector, does not call real Spring AI
EmbeddingModel, and does not call Spring AI VectorStore. RAG evidence remains evidence-only and never executes refund,
exchange, coupon compensation, payment, logistics, or dispute-closure actions. Default tests remain offline and do not
require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, or external network.
Real PGvector and real embedding providers remain opt-in / future paths.

V4.6.1 status: offline deterministic RAG evaluation cases and metrics completed. The project now includes
`docs/evaluation/rag_policy_cases.jsonl`, RAG evaluation models, a JSONL loader, deterministic fixture data, and
`RagEvaluationApplicationService` for KEYWORD / VECTOR / HYBRID policy evidence retrieval evaluation. V4.6.1 does not
add runtime features, does not change `search_aftersale_policy` retrieval logic, does not write ToolCallTrace,
AgentWorkspace, Execution Tree, Ticket, or AgentRun state, and does not use LLM-as-judge. The default runner uses
`FakeEmbeddingClient`, `InMemoryPolicyVectorRepository`, and in-memory keyword policy data. It does not call real LLMs,
real embedding providers, Spring AI, Spring AI VectorStore, PostgreSQL / PGvector, Docker, MySQL, Redis, API keys, raw
datasets, or external network.

V4.6.3 status: RAG Actuator health indicators completed as offline readiness diagnostics. Health indicators may report
RAG search, vector-store provider, embedding provider, and ingestion contract readiness, but they do not change this
retrieval contract, do not execute `search_aftersale_policy`, do not call `EmbeddingClient.embed`, do not call
`PolicyVectorRepository.search`, do not connect to PostgreSQL / PGvector, and do not call Spring AI `VectorStore` or
`EmbeddingModel`. Health status is diagnostic metadata, not retrieval evidence and not business decision confidence.

V4.6.4 status: OpenAPI / API docs polish completed. OpenAPI describes existing HTTP APIs and documents the
`search_aftersale_policy` evidence-only boundary, but it does not add a policy-search HTTP Controller, does not change
this retrieval contract, does not modify KEYWORD / VECTOR / HYBRID algorithms, does not execute tools, and does not
call embedding providers or vector stores. Swagger UI is documentation only and is not production deployment coverage.

Project Review Stage 5 RAG Quality Evaluation: RAG quality improvement evaluation completed as documentation and decision
only. See `docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md`. The current runtime remains
KEYWORD / VECTOR / HYBRID policy evidence retrieval through `search_aftersale_policy`. Reranking is not implemented,
query rewriting is not implemented, RRF is not implemented, and chunk window expansion is not implemented. Future RAG
quality improvements must remain evidence-only and must not bypass ToolRegistry / RiskPolicy / Approval / Trace /
Workspace / Execution Tree.

V5.A.1 status: an explicit opt-in `JdbcPolicyVectorRepository` infrastructure adapter now exists for the
`rag-postgres` / `pgvector` profile. This updates the repository implementation status only. It does not change
`search_aftersale_policy` retrieval algorithms, does not add live PGvector validation to default tests, does not call
Spring AI `VectorStore`, does not add a public RAG HTTP endpoint, and does not turn policy evidence into a business
action. Default tests continue to use fake / in-memory dependencies and remain offline.

V5.A.2 status: `schema-rag-postgres.sql` now declares schema version baseline `2026-06-01-001` for the opt-in
`JdbcPolicyVectorRepository` / PGvector policy evidence search path. The retrieval contract therefore has two explicit
storage paths: default fake / in-memory dependencies for offline deterministic validation, and an opt-in
`JdbcPolicyVectorRepository` path for PGvector-backed policy evidence search. The baseline is not a Flyway / Liquibase
migration framework and does not prove live PGvector connectivity; V5.A.3 is the planned live PGvector smoke test.
RAG evidence remains evidence-only and never executes business actions.

允许链路：

```text
AgentSkill / Specialist Handler
→ ToolRegistry
→ search_aftersale_policy
→ PolicyApplicationService
→ PolicyHybridSearchService
→ KeywordRepository + VectorRepository
→ PolicySearchResult
→ ToolCallTrace
→ AgentWorkspace.PolicyEvidence
→ Execution Tree
```

禁止链路：

```text
AgentSkill → VectorStore
SpecialistAgentHandler → VectorStore
AgentApplicationService → PGvector repository
LLM Planner → VectorStore
ToolExecutor → external LLM direct business action
```

## 3. Tool Definition

```text
toolName: search_aftersale_policy
riskLevel: LOW
requiresApproval: false
type: read-only evidence retrieval
```

该工具不得修改 Ticket、Order、Payment、Inventory、Logistics、Coupon 或 ApprovalRequest 状态。

## 4. Input Schema

V4.5.3 wires this schema into `search_aftersale_policy` runtime while keeping old KEYWORD input compatible.

```json
{
  "query": "质量问题 退货 退款",
  "retrievalMode": "HYBRID",
  "topK": 5,
  "minScore": 0.65,
  "category": "RETURN",
  "productType": "electronics",
  "effectiveAt": "2026-05-27",
  "subtaskId": "S1"
}
```

字段说明：

```text
query: required, non-blank
retrievalMode: KEYWORD | VECTOR | HYBRID, default KEYWORD
topK: bounded integer, default 5, maximum 20
minScore: optional threshold between 0.0 and 1.0
category: optional, narrows policy category
productType: optional, narrows product type
effectiveAt: optional, selects policy effective date
subtaskId: optional trace attribution field
```

## 5. Output Schema

V4.5.3 adds this RAG evidence shape to `search_aftersale_policy` output while preserving the legacy `results` field.

```json
{
  "results": [
    {
      "evidenceId": "evidence-001",
      "policyId": null,
      "documentId": "policy-doc-001",
      "chunkId": "chunk-001",
      "documentTitle": "售后退货退款政策",
      "category": "RETURN",
      "productType": "electronics",
      "snippet": "质量问题在签收后七天内可申请退货退款...",
      "score": 0.82,
      "keywordScore": 0.73,
      "vectorScore": 0.82,
      "retrievalMode": "HYBRID",
      "source": "MERGED_HYBRID",
      "effectiveFrom": "2026-01-01",
      "effectiveTo": null,
      "metadata": {
        "chunkIndex": 3,
        "sourceType": "markdown"
      }
    }
  ],
  "message": "Found 1 policy evidence chunk.",
  "fallbackUsed": false
}
```

## 6. Retrieval Modes

V4.5.3 wires `RetrievalMode` into `search_aftersale_policy` runtime. Missing `retrievalMode` remains equivalent to
KEYWORD for backward compatibility.

### KEYWORD

- 使用 deterministic keyword matching；
- 默认测试必须覆盖；
- 不依赖 embedding provider 或 vector store；
- 可作为 VECTOR 不可用时的 fallback。

### VECTOR

- 使用 embedding 相似度搜索；
- 仅在 `EmbeddingClient` abstraction 和 `PolicyVectorRepository` contract 可用时启用；
- 默认测试使用 fake embedding / in-memory vector repository；
- 缺少 vector dependency 时返回清晰 empty / failure message；
- live vector test 必须显式 opt-in。

### HYBRID

- 合并 keyword 和 vector results；
- 去重规则以 chunkId、policyId、normalized snippet 为准；
- score merge 使用 keywordWeight / vectorWeight 的 deterministic weighted average，并保留 keywordScore / vectorScore；
- fallbackUsed 必须标明 keyword-only、vector-only 或 both-empty fallback。
- vector side unavailable or failed falls back to keyword evidence when keyword evidence exists.

## 7. Evidence Rules

每条 evidence 必须包含：

```text
evidenceId
policyId optional
documentId optional
chunkId optional
documentTitle optional
category
snippet
score or deterministic ranking reason
retrievalMode
source
effectiveFrom/effectiveTo when available
```

Score is a retrieval evidence score, not business-decision confidence. Evidence may cite policy text, but it must not
state that refund, exchange, coupon compensation, payment change, logistics change, inventory change, or dispute
closure has already been completed.

不得返回：

- API Key；
- database password；
- full raw private document；
- full prompt；
- sensitive credentials；
- unbounded long raw text。

## 8. Unsupported Query Behavior

Unsupported query 必须返回结构化空结果：

```json
{
  "results": [],
  "message": "No supported after-sale policy evidence found for query.",
  "fallbackUsed": false
}
```

不得编造政策依据。

## 9. AgentWorkspace 写入规则

RAG result 进入 workspace 时，应写入：

```text
PolicyEvidence
- evidenceId optional
- policyId optional
- chunkId
- documentId
- documentTitle
- category
- productType optional
- snippet
- score optional
- retrievalMode
- source optional
- subtaskId
- toolCallTraceId optional
```

Workspace stores only the current AgentRun policy evidence summary. It must not store full rawText, full chunk content,
full prompt, API keys, passwords, tokens, local paths, or long raw text. Workspace 不替代 ToolCallTrace。ToolCallTrace
仍是实际 tool audit record。

## 10. Execution Tree 展示规则

Execution Tree 应展示：

```text
PolicyEvidenceNode
- evidenceId
- policyId
- chunkId
- documentId
- documentTitle
- category
- productType
- score
- retrievalMode
- source
- snippet
- attachedSubtaskId
- attachedToolCallId
- attachedSkillName optional
```

Execution Tree evidence output is read-only. Querying it must not modify Ticket, AgentRun, ToolCallTrace,
ApprovalRequest, Workspace, or retrieval state. JSON parse failures degrade safely and must not break the whole tree.

## 11. Ingestion Contract

V4.4.1 defines the ingestion domain and repository contract only. V4.4.2 adds deterministic chunking, token estimate,
SHA-256 checksum, and checksum dedup service boundaries. V4.4.3 adds the fake-provider embedding pipeline boundary for
offline tests and writes through the vector repository contract only. It does not implement ingestion database tables,
real embedding generation, JDBC vector writes, Admin API, Agent tool registration, or RAG retrieval runtime.

### V4.4 Ingestion Pipeline Foundation

V4.4 ingestion prepares upstream policy evidence data for future retrieval. It is not Agent runtime, is not registered
in ToolRegistry, and is not called by `search_aftersale_policy` yet. The current foundation can model ingestion runs,
chunk policy text, compute checksums, make dedup decisions, use `FakeEmbeddingClient` in default tests, and write
records through the `PolicyVectorRepository` contract with in-memory infrastructure.

V4.5.3 is the first phase where `search_aftersale_policy` uses KEYWORD / VECTOR / HYBRID runtime modes. Missing
`retrievalMode` still defaults to KEYWORD. V4.5.4 makes the resulting RAG evidence visible in ToolCallTrace output
JSON, AgentWorkspace policy evidence summaries, AgentRun final summary, and Execution Tree read-only evidence
summaries.

RAG retrieval results remain policy evidence only. They must not be represented as refund, exchange, payment,
logistics, coupon, or dispute actions already completed.

Policy ingestion 必须可追踪：

```text
PolicyIngestionRun
- runId
- source
- status
- documentCount
- chunkCount
- embeddedCount
- failedCount
- errorMessage
- startedAt
- finishedAt
```

同一 document checksum 重复导入时不得重复生成 chunk 和 embedding，除非显式 version 更新。

V4.4.2 chunking / checksum rules:

```text
chunkIndex starts at 0
tokenEstimate = ceil(chars / tokenEstimateDivisor)
document checksum = SHA-256(normalized rawText)
chunk checksum = SHA-256(normalized chunk content)
normalization = line-ending normalization + trim
dedup decisions = NEW_CONTENT / DUPLICATE_DOCUMENT / DUPLICATE_CHUNK
```

Chunking and dedup errors must not include complete raw text, API keys, database passwords, tokens, full prompts, or
local absolute paths.

V4.4.3 fake embedding pipeline rules:

```text
eligible run status = CHUNKED / EMBEDDING
CHUNKED -> EMBEDDING before embedding work
all chunks embedded -> COMPLETED
some embedded or skipped plus failures -> PARTIALLY_FAILED
all chunks failed -> FAILED
default provider for tests = FakeEmbeddingClient
vector write boundary = PolicyVectorRepository contract
duplicate embedding = skip or fail according to options
dimension mismatch = fail or skip according to options
```

Embedding pipeline failures must not include complete chunk content, API keys, database passwords, tokens, full prompts,
local absolute paths, or provider secrets.

V4.4.1 status transitions:

```text
CREATED -> RUNNING / CANCELLED
RUNNING -> CHUNKED / FAILED / CANCELLED
CHUNKED -> EMBEDDING / FAILED / CANCELLED
EMBEDDING -> COMPLETED / PARTIALLY_FAILED / FAILED / CANCELLED
COMPLETED / FAILED / PARTIALLY_FAILED / CANCELLED -> terminal
```

Policy Ingestion is an admin / pipeline capability. It must not be exposed as an Agent runtime tool without a separate
security and execution-boundary decision.

## 12. Testing Contract

默认测试必须覆盖：

- keyword retrieval；
- fake vector retrieval；
- hybrid merge；
- unsupported query empty result；
- duplicate chunk merge；
- ToolRegistry execution；
- ToolCallTrace output shape；
- Workspace PolicyEvidence write；
- Execution Tree evidence node；
- no real provider dependency。

Live tests 必须显式 opt-in。

### V4.6.1 RAG Evaluation Contract

RAG evaluation is retrieval evaluation, not Agent flow evaluation. It may call `RagPolicySearchApplicationService`
directly with deterministic fake / in-memory dependencies. It must not create tickets, create AgentRuns, call
ToolRegistry for Agent runtime, write ToolCallTrace, write AgentWorkspace, or mutate Execution Tree state.

The versioned dataset is:

```text
docs/evaluation/rag_policy_cases.jsonl
```

The evaluation report must expose deterministic metrics:

```text
totalCases
passedCases
failedCases
passRate
evidenceRecallPassRate
evidenceSourcePassRate
retrievalModePassRate
fallbackAccuracy
emptyResultAccuracy
citationCompletenessRate
safetyPassRate
averageEvidenceCount
failures
```

Metric checks are exact-field and substring checks. V4.6.1 does not use LLM-as-judge, semantic grading, external
evaluation frameworks, real provider calls, PGvector, Docker, MySQL, Redis, API keys, raw datasets, or external
network. Failures must include caseId, field, expected, actual, and a short sanitized message without full evidence
JSON, full prompt, full chunk content, API keys, passwords, tokens, local paths, or raw dataset paths.

## 13. Risk Boundary

`search_aftersale_policy` remains LOW risk.

RAG evidence 不得声称：

- 已退款；
- 已换货；
- 已补偿；
- 已修改支付；
- 已修改物流；
- 已关闭争议；
- 已执行任何真实高风险业务动作。
