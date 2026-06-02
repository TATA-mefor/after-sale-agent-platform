# Project Review Correction Stage 3.4: Async / Streaming / Batch API Evaluation

Date: 2026-06-01

Status: Completed

## Goal

Evaluate async AgentRun, status polling, SSE / WebSocket streaming, batch API, cancel / retry, and AgentRun list
pagination without implementing new runtime API behavior.

## Scope Completed

- Added the async / streaming / batch API decision record.
- Updated README, OpenAPI docs, remediation plan, active correction plan, quality score, validation commands, and API
  completeness decision.
- Added docs harness coverage for the decision, links, boundaries, and secret / path safety.

## What Changed

- `docs/decisions/DECISION_PROJECT_REVIEW_ASYNC_STREAMING_BATCH_API.md` records Stage 3.4 evaluation results.
- Existing docs now state Stage 3.4 is completed as a decision/evaluation phase.
- Docs explicitly keep async AgentRun runtime, SSE / WebSocket runtime, batch API runtime, cancel / retry, AgentRun list
  pagination, production auth / RBAC, and production API hardening as future work.

## Async AgentRun Evaluation Boundary

Async AgentRun is not implemented. The decision records that a future implementation needs a state machine, executor /
queue strategy, idempotent run requests, duplicate submission protection, failure recovery, trace ordering, approval
state preservation, and offline tests.

## Status Polling Boundary

Current `GET /api/agent-runs/{runId}` status polling remains the safe read path. It is read-only, returns safe summary
fields, links trace and execution-tree views, and does not run Planner, call ToolRegistry, write ToolCallTrace, mutate
Workspace, or modify Ticket / Approval state.

## SSE / WebSocket Evaluation Boundary

SSE / WebSocket streaming is not implemented. Future streaming must expose only safe event summaries and must not
expose raw prompt, raw LLM response, API keys, complete tool output, complete evidence chunks, local paths, or provider
configuration. Streaming must not replace ToolCallTrace or bypass Approval.

## Batch API Evaluation Boundary

Batch API runtime is not implemented. Future batch APIs require size limits, idempotency, rate limiting, partial
failure handling, per-item audit records, approval backlog control, and permission checks.

## Cancel / Retry Boundary

Cancel / retry APIs are not implemented. Future design must define allowed state transitions, idempotency, side-effect
boundaries, retry trace linkage, completed / failed / waiting-approval behavior, and execution-tree visibility.

## ToolRegistry / Planner Boundary

ToolRegistry remains the Agent tool execution entry. LLM output may plan tools but must not directly execute tools.
Async, streaming, batch, cancel, and retry designs must not expose an HTTP bypass around ToolRegistry, RiskPolicy,
Approval, ToolCallTrace, Workspace, Execution Tree, or `search_aftersale_policy`.

## Security / Auth Boundary

Production auth / RBAC is not implemented. It is a prerequisite for future streaming, batch, cancel / retry, trace,
execution-tree, and approval hardening. Current docs do not claim production API security is complete.

## Runtime Non-change Boundary

Stage 3.4 changes docs and docs harness tests only. It does not modify `src/main/java`, controllers, DTO runtime
behavior, services, ToolRegistry, Planner, RAG runtime, ingestion, health indicators, OpenAPI config, ToolCallTrace,
Workspace, Execution Tree, AgentApplicationService, `pom.xml`, or application resources.

## Default Offline Boundary

Docs harness tests only read repository files. Default validation does not require real LLMs, API keys, PostgreSQL,
PGvector, Docker, MySQL, Redis, real embedding providers, Spring AI live provider calls, queues, streaming servers, or
external network.

## Validation Commands

```bash
mvn test -Dtest=AsyncStreamingBatchApiDecisionDocsTest,AgentRunStatusDocsTest,TicketPaginationDocsTest,ApiCompletenessDecisionDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Async AgentRun runtime is not implemented.
- SSE / WebSocket runtime is not implemented.
- Batch API runtime is not implemented.
- Cancel / retry APIs are not implemented.
- AgentRun list pagination is not implemented.
- Production auth / RBAC, idempotency, rate limiting, and production API audit hardening remain future work.
- Real refund, exchange, coupon compensation, payment, logistics, and dispute closure integrations remain future work.

## Follow-ups

- Stage 4: domain model hardening.
- Future API hardening: AgentRun list pagination.
- Future API hardening: async AgentRun execution model.
- Future API hardening: cancel / retry design.
- Future API hardening: SSE after auth / RBAC.
- Future API hardening: batch API after idempotency, rate limiting, and partial-failure design.

## Completion Signal

TASK_COMPLETE
