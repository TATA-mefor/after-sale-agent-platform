# EXEC_PLAN_V3_OBSERVABILITY

Date: 2026-05-17
Status: Completed

## Goal

Implement V3.3 Structured Logging / Observability so one request can be correlated across Ticket creation, AgentRun
execution, specialist subtasks, tool calls, approval records, and execution tree queries without adding an external
observability platform or changing business semantics.

## Scope Completed

- Added `X-Request-Id` request header support.
- Generated a request id when the caller does not provide one.
- Returned `X-Request-Id` on every HTTP response handled by the filter.
- Stored request-level `requestId` in MDC during request handling and cleared it after completion.
- Added shared observability constants for:
  - `requestId`
  - `ticketId`
  - `agentRunId`
  - `subtaskId`
  - `toolName`
  - `approvalRequestId`
- Added a scoped MDC helper for short-lived business IDs.
- Added logging pattern fields in `application.yml`.
- Added structured logs on key application paths:
  - Ticket creation and status changes
  - AgentRun start, plan validation, completion, failure, and approval-required paths
  - Specialist Handler start, completion, and approval-required paths
  - ToolRegistry tool invocation, completion, and failure
  - ApprovalRequest creation, approve, and reject
  - Execution Tree query start and completion
- Added tests for request id generation, propagation, MDC cleanup, and logging pattern coverage.
- Updated README, V3 execution plan, quality score, and the active V3 plan.

## Design

V3.3 uses Spring Boot default logging plus SLF4J MDC. The request filter owns only the request-scoped `requestId`.
Application services and tool execution paths attach business IDs through short-lived MDC scopes around individual log
statements or operation blocks.

This keeps logs useful for diagnosis while preserving the existing audit surfaces:

```text
HTTP response X-Request-Id
-> logs with requestId
-> ticketId / agentRunId / subtaskId / toolName / approvalRequestId
-> ToolCallTrace API and Execution Tree API for audit details
```

Logs intentionally contain identifiers, status, type, risk level, counts, latency, and error class information. They do
not log API keys, database passwords, full LLM prompts, sensitive credentials, or long raw user messages.

## Boundaries Preserved

- No Prometheus, Grafana, ELK, OpenTelemetry, external logging service, Redis, or messaging platform was introduced.
- Default `mvn test` does not require Docker, MySQL, Redis, real LLMs, API keys, or external network.
- ToolCallTrace remains the tool audit record.
- Execution Tree remains the read-only inspection API.
- ApprovalRequest remains the approval state record.
- Agent, ToolRegistry, Approval, Trace, Workspace, Planner, Specialist Handler, and persistence business semantics are
  unchanged.

## Validation Requirements

The completion gate for this phase remains:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Risks

- Logs are only as complete as the code paths that emit them; future application services should add the same MDC
  fields when they introduce new Agent or approval flows.
- MDC is thread-local; future async execution will need explicit context propagation.
- Logging remains diagnostic and should not be treated as the source of truth for business state.

## Follow-ups

- V3.4 should complete the final system review and reconcile README, quality notes, demo flow, and known limitations.
- A later plan can add metrics or distributed tracing if the project introduces async workers or multiple deployable
  services.
- Future logging changes should keep sensitive values and long raw text out of log messages.

## Completion Signal

TASK_COMPLETE
