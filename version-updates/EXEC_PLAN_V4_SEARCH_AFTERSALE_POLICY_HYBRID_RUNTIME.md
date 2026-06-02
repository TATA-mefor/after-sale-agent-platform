# EXEC_PLAN_V4_SEARCH_AFTERSALE_POLICY_HYBRID_RUNTIME

Date: 2026-05-27
Status: Completed

## Goal

Implement V4.5.3 runtime wiring so `search_aftersale_policy` supports KEYWORD, VECTOR, and HYBRID retrieval modes
while preserving the existing LOW-risk read-only tool boundary and old KEYWORD-compatible input behavior.

## Scope Completed

- Extended `search_aftersale_policy` input parsing for optional RAG fields.
- Added RAG policy search application runtime dispatch for KEYWORD, VECTOR, and HYBRID modes.
- Kept old input compatible by defaulting missing `retrievalMode` to KEYWORD.
- Added RAG evidence fields to tool output while preserving legacy `results`.
- Added runtime, ToolRegistry, fallback, safety, and architecture tests.
- Updated V4 plans, README, tool contract docs, RAG retrieval contract docs, and quality docs.

## What Changed

V4.5.3 wires the previously prepared V4.5.1 search contracts and V4.5.2 merge service into the
`search_aftersale_policy` tool runtime. KEYWORD mode uses existing deterministic keyword retrieval. VECTOR mode uses
the `EmbeddingClient` abstraction and `PolicyVectorRepository.search` contract when both are available. HYBRID mode
combines keyword and vector evidence through `RagPolicyEvidenceMergeService`.

## Runtime Mode Boundary

The tool accepts optional `retrievalMode`, `topK`, `minScore`, `category`, `productType`, `effectiveAt`, and
`embeddingModel` fields. Missing `retrievalMode` defaults to KEYWORD, so existing callers keep their previous behavior.
Unknown modes and invalid bounds return clear LOW-risk tool failures without sensitive details.

## KEYWORD Mode Boundary

KEYWORD mode calls the existing keyword policy retrieval path and maps the result into RAG evidence. It does not call
`EmbeddingClient`, does not call `PolicyVectorRepository.search`, and remains compatible with legacy `results` output.

## VECTOR Mode Boundary

VECTOR mode generates a query embedding through the `EmbeddingClient` abstraction and searches through the
`PolicyVectorRepository` contract when those dependencies are available. Default tests use `FakeEmbeddingClient` and
`InMemoryPolicyVectorRepository`. Missing vector dependencies, empty vector results, and embedding failures return
clear sanitized messages.

## HYBRID Mode Boundary

HYBRID mode executes keyword retrieval, attempts vector retrieval, and merges evidence with
`RagPolicyEvidenceMergeService`. When the vector side is unavailable or fails, HYBRID falls back to keyword evidence
and marks `fallbackUsed` clearly. It does not fabricate evidence.

## ToolRegistry Boundary

`search_aftersale_policy` remains the only Agent-facing policy retrieval tool. It stays registered as LOW risk,
read-only, and approval-free. V4.5.3 does not add new tools, does not bypass ToolRegistry, and does not change
ToolRegistry execution semantics.

## Evidence-only Boundary

RAG output is policy evidence only. Scores are retrieval evidence scores, not business decision confidence. The tool
does not execute or claim completion of refunds, exchanges, coupon compensation, payment changes, logistics changes,
inventory changes, or dispute closure.

## Default Offline Test Boundary

Default tests remain offline. VECTOR and HYBRID runtime tests use fake embedding and in-memory vector repository
dependencies. Default validation does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis,
real embedding providers, Spring AI provider calls, Spring AI `VectorStore`, or external network.

## Architecture Boundary

The search tool runtime and RAG application service depend on `EmbeddingClient` abstraction and
`PolicyVectorRepository` contract only. They do not depend on Spring AI provider classes, Spring AI `VectorStore`,
JDBC, `DataSource`, PGvector infrastructure, or vector repository implementations. Agent, Handler, and Skill layers do
not directly depend on embedding clients, vector repositories, or vector infrastructure.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- V4.5.3 does not modify ToolCallTrace schema.
- V4.5.3 does not modify AgentWorkspace evidence write logic.
- V4.5.3 does not add Execution Tree evidence nodes.
- V4.5.3 does not implement `JdbcPolicyVectorRepository` or live PGvector search.
- V4.5.3 does not make real Spring AI embedding calls part of the default test path.
- V4.5.3 does not add Admin Controller or ingestion tool.

## Follow-ups

- V4.5.4 wires ToolCallTrace / Workspace evidence output more deeply.
- Later opt-in work can add live PGvector repository and real-provider embedding integration without changing the
  default offline test boundary.

## Completion Signal

TASK_COMPLETE
