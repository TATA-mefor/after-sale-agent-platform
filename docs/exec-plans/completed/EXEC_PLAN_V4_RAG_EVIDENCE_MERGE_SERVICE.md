# EXEC_PLAN_V4_RAG_EVIDENCE_MERGE_SERVICE

Date: 2026-05-27
Status: Completed

## Goal

Implement the V4.5.2 keyword + vector merge service that combines already prepared RAG policy evidence before the
future `search_aftersale_policy` HYBRID runtime wiring.

## Scope Completed

- Added `RagPolicyEvidenceMergeOptions`.
- Added `RagPolicyEvidenceMergeService`.
- Added merge option, score merge, dedup, fallback, safety, mapper integration, docs harness, and architecture tests.
- Updated V4 plans, README, tool contract docs, RAG retrieval contract docs, and quality docs.

## What Changed

V4.5.2 adds pure merge logic over supplied `RagPolicySearchResult` values. It can produce a HYBRID
`RagPolicySearchResult` from KEYWORD and VECTOR evidence, but it does not execute retrieval.

## Merge Service Boundary

The merge service accepts existing keyword and vector RAG evidence and returns HYBRID evidence. It does not call
repositories, `EmbeddingClient`, `PolicyVectorRepository.search`, Spring AI, VectorStore, PGvector, PostgreSQL, or
external services.

## Score Merge Boundary

The score merge rule is deterministic:

- merged keyword + vector score uses weighted average from keywordScore and vectorScore;
- keyword-only and vector-only evidence retain their available side score through the same weighted-average rule;
- final scores are normalized to 0.0 through 1.0;
- keywordScore and vectorScore remain retrieval evidence scores, not business decision confidence.

## Dedup Boundary

Dedup supports:

- same chunkId;
- same policyId;
- same normalized snippet using trim, whitespace collapse, and lower-case.

Merged evidence preserves available policyId, documentId, chunkId, keywordScore, and vectorScore without fabricating
missing IDs.

## Fallback Boundary

Fallback behavior is explicit:

- keyword-only returns HYBRID evidence with fallbackUsed true and a vector-side-empty message;
- vector-only returns HYBRID evidence with fallbackUsed true and a keyword-side-empty message;
- both-empty and null inputs return empty HYBRID evidence with a clear message.

Fallback messages do not include prompts, API keys, passwords, tokens, local paths, or raw text.

## Runtime Wiring Boundary

V4.5.2 does not change `search_aftersale_policy` runtime, ToolRegistry execution, ToolCallTrace output,
AgentWorkspace writes, AgentRun flow, Skill runtime, or Execution Tree behavior. V4.5.3 owns HYBRID runtime wiring.

## Default Offline Test Boundary

Default tests remain offline and do not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real
embedding providers, Spring AI provider calls, or external network.

## Architecture Boundary

Architecture tests keep the RAG merge service free of Spring Web, JDBC, `DataSource`, Spring AI, VectorStore, PGvector
infrastructure, repository dependencies, and `EmbeddingClient`. Agent, Handler, and Skill layers must not depend on the
merge service.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No `search_aftersale_policy` HYBRID runtime wiring is implemented in V4.5.2.
- No keyword repository search or vector repository search is executed by the merge service.
- No `EmbeddingClient` call, Spring AI VectorStore call, PGvector live search, ToolCallTrace evidence wiring, or
  Workspace evidence wiring is added.

## Follow-ups

- V4.5.3 wires `search_aftersale_policy` to HYBRID mode.
- V4.5.4 wires ToolCallTrace / Workspace evidence output.

## Completion Signal

TASK_COMPLETE
