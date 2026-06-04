# V5.B.3.4 Tracing / Correlation Boundary

Date: 2026-06-04

Status: Completed

## Goal

Add a local HTTP correlation boundary for `X-Correlation-Id` and `X-Request-Id` while preserving default offline
validation, health-only default Actuator exposure, low-cardinality metrics policy, and existing Agent / Tool / Trace
runtime semantics.

## Scope Completed

- Added safe correlation and request identifier handling.
- Added response headers for sanitized `X-Correlation-Id` and `X-Request-Id`.
- Added MDC keys `correlationId` and `requestId`.
- Added structured logging pattern support for both MDC keys.
- Preserved backward-compatible `RequestIdFilter` type.
- Added runtime and observability boundary tests.
- Added documentation and docs harness coverage.
- Updated V5.B.3 status docs.

## What Changed

The project can now correlate local HTTP requests through both correlation and request identifiers in logs. Unsafe
incoming header values are replaced with generated safe values before they reach MDC or response headers.

## Correlation ID Boundary

`X-Correlation-Id` is accepted only when it matches the safe value policy. Missing or unsafe values are replaced with a
generated identifier. Unsafe values are not echoed and are not stored in MDC.

## Request ID Boundary

`X-Request-Id` keeps the existing request-id behavior while using the same safety policy as correlation IDs. The
response always contains a safe `X-Request-Id`.

## MDC Boundary

The filter writes only `correlationId` and `requestId` for this feature and clears both after request processing.
Business identifiers remain outside this feature boundary.

## HTTP Header Boundary

The filter reads only `X-Correlation-Id` and `X-Request-Id`, then returns sanitized safe values for both response
headers. It does not parse provider credentials, prompt content, user identity, ticket identity, order identity, or
AgentRun identity from headers.

## Structured Logging Boundary

The default log pattern now includes `correlationId` and keeps `requestId`. Logs remain diagnostic only; ToolCallTrace,
ApprovalRequest, Workspace, and Execution Tree remain the audit and explanation surfaces.

## AgentRun / ToolCallTrace / ExecutionTree Boundary

V5.B.3.4 does not modify AgentRun state transitions, ToolRegistry execution, `search_aftersale_policy`, RAG retrieval,
ToolCallTrace schema, Workspace evidence logic, Approval behavior, or Execution Tree runtime.

## Metrics Tag Boundary

Correlation and request identifiers are not metrics tags. The low-cardinality Micrometer policy remains unchanged.

## Secret Safety Boundary

Header values are rejected when they are blank, too long, contain unsafe characters, contain whitespace or control
characters, look like URLs or paths, or look credential-like. Raw unsafe header values are not logged, echoed, or stored
in MDC.

## OpenTelemetry Boundary

V5.B.3.4 does not add OpenTelemetry SDK, spans, exporters, collectors, W3C `traceparent`, Jaeger, Zipkin, distributed
tracing, or cross-service propagation.

## Production Tracing Boundary

This is not production tracing or production monitoring. It does not add dashboards, alerts, scrape jobs, tracing
backend, log aggregation backend, production auth, Kubernetes / Helm, release automation, or deployment hardening.

## Runtime Non-change Boundary

No business runtime behavior changed. The task does not modify retrieval algorithms, RAG evaluation, Actuator health
indicator behavior, OpenAPI behavior, ToolRegistry semantics, ToolCallTrace schema, Workspace evidence logic, Execution
Tree runtime, Controller behavior, or production application service logic.

## Default Offline Boundary

Default validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Spring AI `VectorStore`, Docker Compose, Prometheus server, Grafana,
OpenTelemetry collector, tracing backend, log aggregation backend, or external network.

## Validation Commands

```bash
mvn test -Dtest=CorrelationIdsTest
mvn test -Dtest=CorrelationIdFilterBoundaryTest
mvn test -Dtest=CorrelationObservabilityBoundaryTest
mvn test -Dtest=TracingCorrelationDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Correlation is local HTTP log correlation only.
- No distributed tracing or cross-service propagation is implemented.
- No production tracing backend is configured.
- No production monitoring dashboard is completed.

## Follow-ups

- V5.B.3.5 production monitoring roadmap.
- Future OpenTelemetry / distributed tracing strategy if cross-service topology requires it.
- V5.B.4 auth, Kubernetes / Helm, release and rollback hardening.

## Completion Signal

TASK_COMPLETE
