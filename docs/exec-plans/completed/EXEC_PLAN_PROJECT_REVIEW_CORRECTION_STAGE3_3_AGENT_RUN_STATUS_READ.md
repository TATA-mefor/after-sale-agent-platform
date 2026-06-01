# Project Review Correction Stage 3.3: AgentRun Status Read

Date: 2026-06-01

Status: Completed

## Goal

Add a minimal, safe AgentRun read/status polling model so reviewers and future frontends can query an existing
AgentRun without re-running the planner or mixing trace / execution-tree details into the status response.

## Scope Completed

- Added `GET /api/agent-runs/{runId}`.
- Added a narrow AgentRun status response model.
- Kept existing `POST /api/tickets/{ticketId}/agent-runs`, trace, and execution-tree paths unchanged.
- Updated OpenAPI docs, README, API completeness decision docs, remediation plan, validation docs, quality docs, and
  active correction plan.
- Added regression and docs harness tests for the Stage 3.3 boundary.

## What Changed

The new AgentRun read endpoint returns only safe summary fields:

- `runId`;
- `ticketId`;
- `status`;
- `startedAt`;
- `completedAt`;
- `finalSummary`;
- `failureSummary`;
- `traceAvailable`;
- `executionTreeAvailable`;
- `traceUrl`;
- `executionTreeUrl`.

## AgentRun Read Boundary

`GET /api/agent-runs/{runId}` loads a persisted AgentRun through the existing application service and maps it to a
summary DTO. Missing runs return `AGENT_RUN_NOT_FOUND`.

The response does not expose planner JSON, raw prompt, raw provider output, full workspace snapshot, full trace JSON,
or execution-tree internals.

## Status Polling Boundary

The status endpoint is intended for simple polling. It is not an async job system and does not provide progress events,
cancel/retry, idempotency, or stream semantics.

## Trace / Execution Tree Boundary

ToolCallTrace details remain available only through `GET /api/agent-runs/{runId}/traces`. Execution Tree details remain
available only through `GET /api/agent-runs/{runId}/execution-tree`.

The AgentRun status endpoint returns navigation links to those views but does not inline or recompute them.
The status endpoint does not write ToolCallTrace and does not modify Ticket state.
Execution Tree details remain available only through the dedicated execution-tree endpoint.

## API Compatibility Boundary

Existing AgentRun create/start, trace, and execution-tree paths are unchanged. Stage 3.3 adds one read-only endpoint and
does not rename or remove any existing API.

## ToolRegistry / Planner Boundary

The status endpoint does not run Planner, validate a new plan, execute tools, call ToolRegistry, call
`search_aftersale_policy`, invoke RAG retrieval, or create approval requests. Agent tool execution remains gated by
ToolRegistry and the existing AgentApplicationService runtime path.
The status endpoint does not call ToolRegistry.

## Runtime Scope Boundary

Stage 3.3 does not modify search runtime, retrieval algorithms, ingestion, health indicators, OpenAPI configuration
runtime, ToolCallTrace schema, Workspace logic, Execution Tree runtime, Approval behavior, Spring AI adapters,
PGvector paths, or provider configuration.

## Default Offline Boundary

Default validation continues to use in-memory / fake paths. It does not require real LLMs, API keys, PostgreSQL,
PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live calls, or external network.

## Validation Commands

```bash
mvn test -Dtest=AgentRunStatusDocsTest,TicketPaginationDocsTest,ApiCompletenessDecisionDocsTest
mvn test -Dtest=OpenApiDocumentationTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- No async AgentRun runtime.
- No SSE / WebSocket streaming.
- No batch API.
- No AgentRun cancel/retry API.
- No production auth / RBAC.
- No public RAG HTTP endpoint.
- No production payment, logistics, refund, exchange, or coupon integration.

## Follow-ups

- Stage 3.4: evaluate async AgentRun, SSE / WebSocket streaming, and batch API boundaries.
- Future API hardening: production auth / RBAC, idempotency, rate limiting, operator audit hardening, and API
  versioning.

## Completion Signal

TASK_COMPLETE
