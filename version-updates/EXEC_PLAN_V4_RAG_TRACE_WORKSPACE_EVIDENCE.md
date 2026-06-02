# V4.5.4 ToolCallTrace / Workspace Evidence Wiring

Date: 2026-05-27

Status: Completed

## Goal

Make RAG policy evidence from `search_aftersale_policy` visible in ToolCallTrace output JSON, single-run
AgentWorkspace policy evidence summaries, AgentRun final summary, and Execution Tree read-only views without changing
the KEYWORD / VECTOR / HYBRID retrieval algorithms.

## Scope Completed

- Stabilized `search_aftersale_policy` evidence output for ToolCallTrace audit JSON.
- Mapped RAG `evidences` into `AgentWorkspace.PolicyEvidence` with legacy `results` fallback.
- Added concise policy evidence summary text to AgentRun final summary.
- Added Execution Tree policy evidence summary nodes.
- Added tests for trace output, workspace evidence, final summary, execution tree visibility, handler regression,
  sanitization, and architecture boundaries.

## What Changed

- `PolicyEvidence` can carry RAG evidence identifiers, document/chunk metadata, score, retrievalMode, and source.
- Agent and specialist handler workspace mapping reads RAG `evidences` from `search_aftersale_policy` output.
- Execution Tree response includes read-only policy evidence summaries at root and subtask levels.
- Evidence values are sanitized and truncated before entering workspace summaries and execution tree views.
- V4 roadmap, tool contract, RAG retrieval contract, vector-store decision, quality score, and README now document the
  V4.5.4 boundary.

## ToolCallTrace Evidence Boundary

ToolCallTrace schema and domain semantics remain unchanged. The audit surface is the existing tool output JSON from
`search_aftersale_policy`, which keeps legacy `results` compatibility and includes stable `evidences`,
`retrievalMode`, `fallbackUsed`, `totalKeywordMatches`, and `totalVectorMatches` fields.

The output must not contain API keys, passwords, tokens, local paths, full prompts, rawText, or long chunk content.
Scores are retrieval evidence scores, not business decision confidence.

## Workspace Evidence Boundary

Workspace stores only single-AgentRun policy evidence summaries. It is not long-term memory and does not replace
ToolCallTrace. Workspace evidence may include evidenceId, policyId, documentId, chunkId, documentTitle, category,
productType, snippet, score, retrievalMode, and source when available.

Empty evidence results do not fabricate policy evidence.

## Final Summary Boundary

AgentRun final summary can mention concise policy evidence references such as retrievalMode, category, policyId or
chunkId, documentTitle, score, and a short snippet. It must not include full evidence JSON, full chunk content, full
prompt, rawText, or sensitive values.

Final summary must not claim refund, exchange, coupon compensation, payment, logistics, or dispute closure was
completed because of RAG evidence.

## Execution Tree Evidence Boundary

Execution Tree remains read-only. It can display sanitized policy evidence summaries and associate them with subtask
and tool call metadata when available. Querying Execution Tree must not mutate Ticket, AgentRun, ToolCallTrace,
ApprovalRequest, Workspace, or retrieval state.

Tool output JSON parse failures degrade safely and must not break the whole execution tree response.

## Evidence-only Boundary

RAG evidence is policy retrieval evidence only. It does not execute refunds, exchanges, coupon compensation, payment
changes, logistics changes, inventory changes, or dispute closure. `search_aftersale_policy` remains a LOW-risk
read-only tool executed through ToolRegistry.

## Default Offline Test Boundary

Default tests use fake / in-memory dependencies where vector evidence is exercised. Default validation does not require
real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI provider calls, or
external network.

Default validation does not require real LLMs, API keys, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding providers, or external network.

Real PGvector and real embedding providers remain opt-in / future paths.

## Architecture Boundary

Workspace mapping may depend on RAG search model shapes but not RAG infrastructure. Execution Tree application code
does not depend on `EmbeddingClient`, `PolicyVectorRepository`, PGvector infrastructure, Spring AI `VectorStore`,
JDBC, `DataSource`, or repository implementations.

Agent, Handler, and Skill layers do not directly access embedding clients, vector repositories, PGvector, VectorStore,
JDBC, DataSource, or fake vector repository implementations. ToolRegistry remains the Agent tool execution boundary.

## Validation Commands

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- V4.5.4 does not change retrieval algorithms.
- V4.5.4 does not implement live PGvector search.
- V4.5.4 does not implement `JdbcPolicyVectorRepository`.
- V4.5.4 does not add Admin Controller or ingestion tool.
- V4.5.4 does not make real embedding providers a default test path.
- V4.5.4 does not implement V4.6+ Skill runtime migration, evaluation, demo, or Spring Boot completeness work.

## Follow-ups

- Continue V4.6 evaluation, demo, Spring Boot completeness, or Skill-layer integration work.
- Keep live PGvector and real embedding provider tests opt-in.
- Keep RAG evidence visible as evidence, not as business action execution.

## Completion Signal

TASK_COMPLETE
