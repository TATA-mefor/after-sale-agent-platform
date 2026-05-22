
# Decision: V4 RAG Vector Store

Date: 2026-05-22
Status: Accepted

## Context

V1/V2/V3 的 `search_aftersale_policy` 采用 deterministic in-memory keyword retrieval。该实现适合默认测试和早期 demo，但不能充分体现 RAG、向量化存储、embedding、chunk、evidence citation 和 retrieval evaluation 等面试重点。

V4 需要把政策检索从关键词检索升级为可持久化、可审计、可测试的 RAG 检索系统，同时保留默认离线 deterministic fallback。

## Decision

V4 引入 PostgreSQL + PGvector 作为显式 opt-in 的 RAG vector store profile。

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
policy_ingestion_runs
```

核心领域对象：

```text
PolicyDocument
PolicyChunk
PolicyEmbedding
PolicyIngestionRun
PolicySearchQuery
PolicySnippet
PolicySearchResult
RagPolicyEvidence
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
- RAG 可以进入 ToolCallTrace、Workspace 和 Execution Tree；
- 默认测试继续离线稳定。

Costs:

- 需要新增 PostgreSQL / PGvector profile；
- 需要 migration / schema 管理；
- 需要 embedding provider 边界；
- 需要 ingestion run 状态和失败处理。

## Non-goals

- 不把 PGvector 加入默认测试依赖；
- 不引入大型分布式向量库；
- 不做复杂 reranking service；
- 不做真实生产知识库权限系统；
- 不让 RAG 结果替代审批或业务规则。
