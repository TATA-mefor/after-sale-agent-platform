# EXEC_PLAN_V4_RAG_SEARCH_CONTRACT

Date: 2026-05-27
Status: Completed

## Goal

Define the V4.5.1 RAG policy search contract, retrieval mode model, evidence model, and mapper boundary needed before
the later HYBRID policy search runtime work.

## Scope Completed

- Added the pure `policy.rag.search` contract package.
- Added `RetrievalMode` with KEYWORD, VECTOR, and HYBRID modes.
- Added `RagPolicySearchQuery`, `RagPolicyEvidenceSource`, `RagPolicyEvidence`, and `RagPolicySearchResult`.
- Added keyword and vector result mappers that convert already available results into RAG evidence models.
- Added model, mapper, docs harness, and architecture tests.
- Updated V4 plans, README, tool contract docs, RAG retrieval contract docs, quality docs, and vector-store decision docs.

## What Changed

V4.5.1 is schema preparation only. It creates a stable model shape for future `search_aftersale_policy` KEYWORD /
VECTOR / HYBRID retrieval without changing current tool runtime behavior.

## Retrieval Mode Boundary

`RetrievalMode` supports:

- KEYWORD for the current controlled keyword retrieval path.
- VECTOR for future vector-only evidence retrieval.
- HYBRID for future keyword + vector merged evidence retrieval.

Blank or missing retrieval mode defaults to KEYWORD. Unknown modes fail with a clear validation error.

## RAG Evidence Boundary

`RagPolicyEvidence` represents policy evidence only. Evidence scores are retrieval scores, not business decision
confidence. Evidence snippets and metadata are validated so the contract does not actively claim completed refunds,
exchanges, compensation, or dispute closure and does not carry API keys, passwords, tokens, local paths, full prompts,
or long raw text.

## Mapper Boundary

Keyword and vector mappers only transform supplied results:

- `KeywordPolicyEvidenceMapper` converts existing `PolicySearchResult` values into KEYWORD evidence.
- `VectorPolicyEvidenceMapper` converts supplied `VectorSearchResult` values into VECTOR evidence.

They do not call repositories, `EmbeddingClient`, `PolicyVectorRepository.search`, Spring AI, VectorStore, PGvector,
or any database.

## Tool Contract Preparation Boundary

The docs now describe the future `search_aftersale_policy` input and output schema with `retrievalMode`, topK,
optional filters, evidence IDs, scores, and source markers.

V4.5.1 does not change `search_aftersale_policy` runtime, ToolRegistry execution, ToolCallTrace output,
AgentWorkspace writes, AgentRun flow, Skill runtime, or Execution Tree behavior. `search_aftersale_policy` remains a
LOW-risk read-only tool, and RAG results remain evidence only.

## Default Offline Test Boundary

Default tests remain offline and do not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real
embedding providers, Spring AI provider calls, or external network.

## Architecture Boundary

Architecture tests keep the RAG search contract package free of Spring Web, JDBC, `DataSource`, Spring AI,
VectorStore, PGvector infrastructure, and repository dependencies. Agent, Handler, and Skill layers must not depend on
the V4.5.1 search-preparation models.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No keyword + vector merge service is implemented in V4.5.1.
- No HYBRID runtime wiring is added to `search_aftersale_policy`.
- No `EmbeddingClient` call, `PolicyVectorRepository.search` call, Spring AI VectorStore call, or PGvector connection
  is added.
- No ToolCallTrace or AgentWorkspace evidence wiring is changed.

## Follow-ups

- V4.5.2 implements keyword + vector merge service.
- V4.5.3 wires `search_aftersale_policy` to HYBRID mode.
- V4.5.4 wires ToolCallTrace / Workspace evidence shape.

## Completion Signal

TASK_COMPLETE
