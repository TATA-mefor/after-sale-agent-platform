
# Decision: V4 RAG Vector Store

Date: 2026-05-22
Status: Accepted

## Context

V1/V2/V3 的 `search_aftersale_policy` 采用 deterministic in-memory keyword retrieval。该实现适合默认测试和早期 demo，但不能充分体现 RAG、向量化存储、embedding、chunk、evidence citation 和 retrieval evaluation 等面试重点。

V4 需要把政策检索从关键词检索升级为可持久化、可审计、可测试的 RAG 检索系统，同时保留默认离线 deterministic fallback。

## Decision

V4 引入 PostgreSQL + PGvector 作为显式 opt-in 的 RAG vector store profile。

V4.3.1 implementation status: the PostgreSQL / PGvector dependency and profile boundary is completed. The project has
a PostgreSQL JDBC runtime dependency, default-off PGvector properties, an explicit `rag-postgres` profile, and a
sanitized profile guard that validates opt-in configuration without connecting to PostgreSQL.

V4.3.2 implementation status: the vector schema file and repository contract are completed. The project now has
`schema-rag-postgres.sql`, pure RAG vector domain models, and a `PolicyVectorRepository` interface. There is still no
JDBC repository, Spring AI VectorStore usage, vector search runtime, policy ingestion, RAG runtime, or Docker Compose
PGvector service.

V4.3.3 implementation status: the fake vector store and default offline vector tests are completed.
`InMemoryPolicyVectorRepository` implements the repository contract with deterministic cosine similarity, filtering,
ranking, empty-result behavior, and duplicate rejection. It is test/local fake infrastructure only and does not change
`search_aftersale_policy`, AgentRun, ToolRegistry, ToolCallTrace, Workspace, or Execution Tree behavior.

V4.3.4 implementation status: the Docker Compose / opt-in PGvector integration documentation boundary is completed.
`docker-compose-rag.yml` starts a local development only PGvector PostgreSQL service, `.env.rag.example` documents
placeholder profile settings, and `docs/demo/V4_PGVECTOR_LOCAL_SETUP.md` documents schema initialization and local
validation steps. There is still no JDBC repository, PGvector live search, policy ingestion, HYBRID retrieval, RAG
runtime, or `search_aftersale_policy` vector wiring.

V4.4.1 implementation status: the Policy Ingestion domain / status / repository foundation is completed.
`PolicyIngestionRun`, ingestion source/document/chunk/error models, `PolicyIngestionStateMachine`,
`PolicyIngestionRepository`, and `InMemoryPolicyIngestionRepository` define the admin/pipeline ingestion boundary.
There is still no chunking service, checksum dedup service, embedding pipeline, vector repository write,
JdbcPolicyIngestionRepository, ingestion API/tool, HYBRID retrieval, RAG runtime, or `search_aftersale_policy` vector
wiring.

V4.4.2 implementation status: the chunking / checksum / dedup service boundary is completed. The project now has
deterministic `PolicyChunkingService`, SHA-256 `PolicyContentChecksumService`, and `PolicyIngestionDedupService`
backed by checksum queries on `PolicyIngestionRepository`. There is still no embedding pipeline, `EmbeddingClient`
call, `PolicyVectorRepository` write, JDBC ingestion repository, ingestion API/tool, HYBRID retrieval, RAG runtime, or
`search_aftersale_policy` vector wiring.

V4.4.3 implementation status: the fake-provider embedding pipeline boundary is completed. The project now has
`PolicyEmbeddingPipelineService`, options, result, and failure models. The pipeline reads ingestion run/document/chunk
state, calls the `EmbeddingClient` abstraction in offline tests with `FakeEmbeddingClient`, writes `PolicyDocument`,
`PolicyChunk`, and `PolicyEmbedding` through the `PolicyVectorRepository` contract, and verifies this path with
`InMemoryPolicyVectorRepository`. There is still no real Spring AI embedding call in default tests, Spring AI
`VectorStore`, PGvector / JDBC repository, ingestion API/tool, HYBRID retrieval, RAG runtime, or
`search_aftersale_policy` vector wiring.

V4.4.4 implementation status: the ingestion documentation and V4.4 completion record are completed.
`docs/demo/V4_POLICY_INGESTION_PIPELINE.md` documents the V4.4 ingestion foundation, default offline path, failure
handling, safety boundary, and future real-provider path. `docs/exec-plans/completed/EXEC_PLAN_V4_POLICY_INGESTION_FOUNDATION.md`
records the total V4.4 completion. There is still no Admin Controller, `ingest_policy_document` tool, ToolRegistry
wiring, real Spring AI embedding default path, JDBC repository, PGvector live write, HYBRID retrieval, RAG runtime, or
`search_aftersale_policy` vector wiring.

V4.5.1 implementation status: the RAG search contract / retrieval mode / evidence model boundary is completed. The
project now has `RetrievalMode`, `RagPolicySearchQuery`, `RagPolicyEvidenceSource`, `RagPolicyEvidence`,
`RagPolicySearchResult`, and keyword/vector result mappers. V4.5.1 is schema preparation only and does not change
`search_aftersale_policy` runtime, implement keyword + vector merge service, call `EmbeddingClient`, call
`PolicyVectorRepository.search`, connect PGvector, call Spring AI VectorStore, modify ToolCallTrace output, or modify
AgentWorkspace writes.

V4.5.2 implementation status: the keyword + vector merge service boundary is completed. The project now has
`RagPolicyEvidenceMergeOptions` and `RagPolicyEvidenceMergeService` for deterministic score merge, dedup, topK,
minScore, and fallback over already supplied KEYWORD / VECTOR evidence. V4.5.2 does not change
`search_aftersale_policy` runtime or execute keyword/vector retrieval.

V4.5.3 implementation status: `search_aftersale_policy` now supports KEYWORD / VECTOR / HYBRID runtime modes while
remaining LOW-risk, read-only, and ToolRegistry-bound. Old calls without `retrievalMode` default to KEYWORD. VECTOR /
HYBRID default tests use `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository`. Real PGvector, real embedding
providers, and Spring AI `VectorStore` are not the default path. ToolCallTrace schema, AgentWorkspace evidence writes,
AgentRun, Skill runtime, and Execution Tree remain unchanged until later wiring.

V4.5.4 implementation status: ToolCallTrace / Workspace evidence wiring is completed. The existing ToolCallTrace
schema remains unchanged, but `search_aftersale_policy` output JSON exposes stable RAG evidence for audit. Workspace
stores single-AgentRun policy evidence summaries, final summary includes concise policy evidence references, and
Execution Tree can display read-only evidence summaries. This wiring does not change retrieval algorithms, does not
connect PGvector, does not call real Spring AI EmbeddingModel, does not call Spring AI `VectorStore`, and does not give
Agent, Handler, or Skill layers direct access to `EmbeddingClient`, `PolicyVectorRepository`, vector infrastructure,
JDBC, `DataSource`, PGvector, or fake vector repository implementations.

推荐 profile：

```text
default      -> in-memory / fake vector repository / fake embedding
mysql        -> V3 MySQL persistence
rag-postgres -> PostgreSQL + PGvector policy vector store
```

Policy Retrieval 升级为 hybrid retrieval：

```text
keyword retrieval
+ vector retrieval
+ evidence merge / optional rerank
+ structured policy evidence output
```

## Data Model

核心表：

```text
policy_documents
policy_chunks
policy_embeddings
```

`policy_ingestion_runs` is not part of the V4.3.2 schema file. V4.4.1 defines the ingestion domain/status/repository
contract in Java only. V4.4.2 adds deterministic chunking, checksum, and dedup services in Java only. V4.4.3 adds an
offline fake-provider embedding pipeline that writes through the repository contract only. Database ingestion schema,
JDBC persistence, real embedding generation, and live PGvector writes remain future work. V4.4.4 closes the ingestion
foundation documentation without changing runtime behavior.

核心领域对象：

```text
PolicyDocument
PolicyChunk
PolicyEmbedding
VectorSearchQuery
VectorSearchResult
VectorSearchMatch
PolicyVectorRepository
CosineSimilarityCalculator
InMemoryPolicyVectorRepository
PolicyChunkingService
PolicyContentChecksumService
PolicyIngestionDedupService
PolicyEmbeddingPipelineService
RetrievalMode
RagPolicySearchQuery
RagPolicyEvidence
RagPolicySearchResult
KeywordPolicyEvidenceMapper
VectorPolicyEvidenceMapper
```

## Retrieval Flow

```text
AgentSkill / Specialist Handler
→ ToolRegistry
→ search_aftersale_policy
→ PolicyApplicationService
→ PolicyHybridSearchService
→ Keyword repository + Vector repository
→ PolicySearchResult
→ ToolCallTrace
→ AgentWorkspace.PolicyEvidence
→ Execution Tree
```

Agent、Handler、Skill 不得直接访问 VectorStore。RAG 检索必须通过 `search_aftersale_policy` tool 或 PolicyApplicationService 边界进入 Agent 链路。

Policy ingestion does not enter ToolRegistry or Agent runtime in V4.4. It is an admin / offline pipeline foundation
for preparing future policy evidence data. This keeps ingestion credentials, raw text handling, and vector writes away
from normal customer-facing AgentRun execution.

V4.5.1 RAG search contracts also do not enter ToolRegistry or Agent runtime. They prepare the future
`search_aftersale_policy` evidence schema while preserving the current keyword-only runtime behavior until V4.5.3.

V4.5.3 RAG search runtime enters Agent execution only through the existing LOW-risk `search_aftersale_policy` tool and
ToolRegistry. Agent, Handler, and Skill layers still do not directly access `EmbeddingClient`,
`PolicyVectorRepository`, vector infrastructure, JDBC, `DataSource`, PGvector, or fake vector repository
implementations.

V4.5.4 evidence observability stays on the same boundary: ToolCallTrace remains the tool audit source of truth,
Workspace stores only single-run evidence summaries, and Execution Tree is a read-only explanation view. None of these
surfaces execute retrieval directly or bypass ToolRegistry.

## Ingestion Flow

```text
policy source document
→ document reader
→ metadata extraction
→ checksum duplicate detection
→ chunking
→ embedding
→ vector store write
→ ingestion run status
```

## Evidence Boundary

RAG 结果只能作为 policy evidence。RAG 不得：

- 直接执行退款；
- 直接执行换货；
- 直接发放优惠券；
- 直接修改订单、支付、库存或物流；
- 直接关闭争议；
- 声称售后动作已经完成。

## Test Strategy

默认测试：

```text
FakeEmbeddingClient
FakeVectorRepository
InMemoryKeywordPolicyRepository
No PostgreSQL
No PGvector
No Docker
No network
```

Live / integration tests：

```bash
mvn test -Dtest=RagVectorStoreLiveTest -Dlive.rag=true
mvn test -Dtest=V4RealAgentRagLiveTest -Dlive.llm=true -Dlive.rag=true
```

## Consequences

Positive:

- 项目具备真实 RAG 工程结构；
- 政策证据可被 chunkId / documentId / score / retrievalMode 追踪；
- RAG evidence can be returned through `search_aftersale_policy`, audited through ToolCallTrace output JSON, summarized
  in AgentWorkspace and final summary, and displayed through Execution Tree read-only evidence nodes;
- 默认测试继续离线稳定。

Costs:

- 需要新增 PostgreSQL / PGvector profile；
- 需要 migration / schema 管理；
- 需要 embedding provider 边界；
- 需要 ingestion run 状态和失败处理。

## Non-goals

- 不把 PGvector 加入默认测试依赖；
- 不在 V4.3.1 中创建 schema、repository、VectorStore search、Policy Ingestion 或 Docker Compose PGvector 服务；
- 不在 V4.3.2 中创建 JDBC repository、调用 EmbeddingClient、调用 Spring AI VectorStore、执行 PGvector similarity
  search、实现 Policy Ingestion 或修改 `search_aftersale_policy` 行为；
- 不在 V4.3.3 中连接 PostgreSQL、实现 JDBC repository、调用 EmbeddingClient、调用 Spring AI VectorStore、实现
  Policy Ingestion、实现 RAG / HYBRID retrieval 或修改 `search_aftersale_policy` 行为；
- 不在 V4.3.4 中实现 JDBC repository、PGvector live search、调用 EmbeddingClient、调用 Spring AI VectorStore、
  实现 Policy Ingestion、实现 RAG / HYBRID retrieval、让 app 默认连接 PGvector 或修改 `search_aftersale_policy`
  行为；
- 不在 V4.4.1 中实现 chunking service、checksum dedup service、调用 EmbeddingClient、写入 PolicyVectorRepository、
  实现 JdbcPolicyIngestionRepository、实现 ingestion API/tool、实现 RAG / HYBRID retrieval 或修改
  `search_aftersale_policy` 行为；
- 不在 V4.4.2 中调用 EmbeddingClient、调用 Spring AI、写入 PolicyVectorRepository、实现
  JdbcPolicyIngestionRepository、实现 JdbcPolicyVectorRepository、实现 Admin Controller、注册 ingestion tool、实现
  RAG / HYBRID retrieval 或修改 `search_aftersale_policy` 行为；
- 不在 V4.4.3 中调用真实 Spring AI EmbeddingModel、调用 SpringAiEmbeddingClient default path、调用 Spring AI
  VectorStore、实现 JdbcPolicyIngestionRepository、实现 JdbcPolicyVectorRepository、连接 PostgreSQL / PGvector、
  实现 Admin Controller、注册 ingestion tool、实现 RAG / HYBRID retrieval 或修改 `search_aftersale_policy` 行为；
- 不在 V4.4.4 中新增 ingestion 运行时代码、修改 chunking/checksum/embedding pipeline 行为、调用 EmbeddingClient、
  调用 PolicyVectorRepository、连接 PostgreSQL / PGvector、实现 Admin Controller、注册 ingestion tool、实现 RAG /
  HYBRID retrieval 或修改 `search_aftersale_policy` 行为；
- 不在 V4.5.1 中实现 keyword + vector merge service、修改 `search_aftersale_policy` runtime、调用
  EmbeddingClient、调用 PolicyVectorRepository.search、调用 Spring AI VectorStore、连接 PostgreSQL / PGvector、
  修改 ToolCallTrace output 或修改 AgentWorkspace writes；
- 不把 PGvector compose 写成 production deployment，`docker-compose-rag.yml` 只用于 local development opt-in；
- 不引入大型分布式向量库；
- 不做复杂 reranking service；
- 不做真实生产知识库权限系统；
- 不让 RAG 结果替代审批或业务规则。
