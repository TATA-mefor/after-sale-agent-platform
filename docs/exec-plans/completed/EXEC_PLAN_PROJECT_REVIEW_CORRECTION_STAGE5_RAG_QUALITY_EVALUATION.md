# Project Review Correction Stage 5: RAG Quality Evaluation

Date: 2026-06-01

Status: Completed

## Goal

对项目审查中关于 RAG 检索质量的结论做事实核验和后续路线决策，明确 reranking、query rewriting、RRF、
chunk window expansion、provider / PGvector path 与现有 KEYWORD / VECTOR / HYBRID baseline 的关系。

## Scope Completed

- 新增 RAG quality improvement decision。
- 更新 RAG retrieval contract、vector decision、evaluation docs、README、整改方案、active plan、quality docs 和
  validation docs。
- 新增 docs harness test。
- 明确阶段 5 只做 evaluation / decision，不做 runtime RAG 质量实现。

## What Changed

- Added `docs/decisions/DECISION_PROJECT_REVIEW_RAG_QUALITY_IMPROVEMENT.md`。
- Added this completion record。
- Documented current KEYWORD / VECTOR / HYBRID RAG baseline。
- Documented deterministic RAG evaluation baseline。
- Documented future evaluation strategy for reranking, query rewriting, RRF, hybrid scoring and chunk window
  expansion。
- Documented provider / PGvector opt-in boundary。

## Current RAG Baseline Boundary

Current baseline:

- `search_aftersale_policy` supports KEYWORD / VECTOR / HYBRID。
- VECTOR uses EmbeddingClient abstraction and PolicyVectorRepository contract。
- HYBRID uses `RagPolicyEvidenceMergeService`。
- Default vector path uses `FakeEmbeddingClient` and `InMemoryPolicyVectorRepository`。
- RAG evidence can surface through ToolCallTrace / Workspace / Execution Tree。

This completion record does not change the runtime retrieval algorithm.

## Evaluation Baseline Boundary

Current RAG evaluation is deterministic, JSONL-based, and offline. It does not use LLM-as-judge by default and does not
call real LLMs, real embedding providers, Spring AI, PostgreSQL / PGvector, Docker, MySQL, Redis, API keys, raw
datasets, or external network.

## Reranking Evaluation Boundary

Reranking is not implemented in stage 5. Future reranking must start with a reranker abstraction, fake deterministic
tests, opt-in real provider wiring, and a clear statement that reranking score is retrieval score, not business
decision confidence.

## Query Rewriting Evaluation Boundary

Query rewriting is not implemented in stage 5. Future rewriting must be auditable, deterministic by default, safe
against raw prompt leakage, and must not treat rewritten query text as the original user statement.

## RRF / Hybrid Scoring Boundary

RRF is not implemented in stage 5. Current HYBRID scoring remains deterministic weighted merge / dedup / fallback.
Future RRF or scoring changes must be proven against deterministic evaluation cases.

## Chunk Window Expansion Boundary

Chunk window expansion is not implemented in stage 5. Future expansion must define max window, max chars, source
citation and dedup rules, and must not return full source documents.

## Provider / PGvector Boundary

`JdbcPolicyVectorRepository` is not implemented. live PGvector validation is not completed. Spring AI VectorStore
production path is not enabled. Real embedding or reranking providers must be opt-in. Default `mvn test` must not
connect PostgreSQL / PGvector.

## ToolRegistry / Evidence-only Boundary

`search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool. RAG evidence is evidence-only and does not
execute refund, exchange, coupon compensation, payment, logistics, inventory or dispute-closure actions. Future RAG
quality improvements must not bypass ToolRegistry, RiskPolicy, Approval, ToolCallTrace, Workspace or Execution Tree.

## Runtime Non-change Boundary

Stage 5 changes docs and docs harness tests only. It does not modify `src/main/java`, `src/main/resources`, `pom.xml`,
RagPolicySearchApplicationService, RagPolicyEvidenceMergeService, SearchAfterSalePolicyToolExecutor,
PolicyVectorRepository, EmbeddingClient, ToolRegistry, ToolCallTrace, Workspace, ExecutionTreeApplicationService,
health indicators, OpenAPI config or ArchitectureTest.

## Default Offline Boundary

Default validation remains offline and deterministic. It does not require real LLMs, API keys, PostgreSQL, PGvector,
Docker, MySQL, Redis, real embedding providers, real reranker providers, Spring AI VectorStore, or external network.

## Validation Commands

```bash
mvn test -Dtest=RagQualityDecisionDocsTest,SpringAiDeepeningDecisionDocsTest,AsyncStreamingBatchApiDecisionDocsTest,ObservabilityHardeningDecisionDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Stage 5 does not improve runtime retrieval quality directly。
- Stage 5 does not expand the runtime evaluation runner。
- Stage 5 does not implement reranking、query rewriting、RRF、chunk window expansion、JdbcPolicyVectorRepository、
  live PGvector validation or Spring AI VectorStore production path。
- Stage 5 does not add a public RAG HTTP endpoint。

## Follow-ups

- Stage 6 remains deployment hardening。
- Expand deterministic RAG evaluation cases before runtime scoring changes。
- Evaluate RRF / hybrid scoring in a separate task。
- Evaluate chunk window expansion in a separate task。
- Define reranker abstraction in a separate task。
- Keep live PGvector and real provider validation opt-in。

## Completion Signal

TASK_COMPLETE
