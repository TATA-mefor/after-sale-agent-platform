# V5.B.3.2 Micrometer Metrics Foundation

Date: 2026-06-03

Status: Completed

## Goal

Add a low-cardinality Micrometer metrics foundation for core application observations while preserving default offline
validation and health-only Actuator exposure.

## Scope Completed

- Added project-owned metric names and low-cardinality tag vocabulary.
- Added a centralized `ApplicationMetricsRecorder` around Micrometer `MeterRegistry`.
- Added tag sanitization for secrets, paths, URLs, prompt/query/snippet-like values, long values, and free text.
- Added AgentRun, ToolCall, Approval, RAG search, and provider metrics recording boundaries.
- Added runtime boundary tests for `MeterRegistry`, recorder bean availability, health-only Actuator exposure, and
  default offline context isolation.
- Added docs and docs harness coverage for B3.2 status, metrics naming, tag safety, actuator exposure, and future work.

## What Changed

- `src/main/java/io/github/tatame/aftersale/common/observability/metrics/` now contains the application metrics
  boundary.
- AgentRun, ToolRegistry, Approval, and RAG search paths now record best-effort metrics through the centralized
  recorder.
- Documentation records V5.B.3.2 as completed and keeps Prometheus, OpenTelemetry, dashboards, and production
  monitoring as planned / future work.

## Micrometer Foundation Boundary

Micrometer is used through Spring Boot Actuator's existing Micrometer core dependency. No Prometheus registry,
OpenTelemetry exporter, dashboard, collector, or monitoring backend is introduced.

Metric recording is best-effort. Recorder failures are swallowed and must not change business runtime behavior.

## Metric Naming Boundary

All project-owned meter names use the `aftersale.*` prefix:

- `aftersale.agent.run.total`
- `aftersale.agent.run.duration`
- `aftersale.tool.call.total`
- `aftersale.tool.call.duration`
- `aftersale.approval.request.total`
- `aftersale.approval.decision.total`
- `aftersale.rag.search.total`
- `aftersale.rag.search.duration`
- `aftersale.provider.call.total`
- `aftersale.provider.call.duration`

## Low-cardinality Tag Boundary

Allowed tags are bounded to component, operation, outcome, status, tool name, risk level, retrieval mode, fallback,
provider type, and approval decision. IDs, raw prompts, search queries, snippets, secrets, credentials, URLs, JDBC
URLs, local paths, raw dataset paths, and private endpoints are not allowed in metric tags.

## AgentRun Metrics Boundary

AgentRun metrics record started/completed counts and duration only. They do not change planning, state transitions,
ToolRegistry execution, ToolCallTrace, Workspace, Execution Tree, or Ticket behavior.

## ToolCall Metrics Boundary

ToolCall metrics are recorded inside the ToolRegistry boundary. They do not bypass ToolRegistry, RiskPolicy, Approval,
or ToolCallTrace and do not modify tool execution semantics.

## Approval Metrics Boundary

Approval metrics record request and decision counts only. They do not change approval request creation, approve/reject
state transitions, or high-risk action boundaries.

## RAG Search Metrics Boundary

RAG metrics record retrieval mode, fallback, outcome, and duration only. They do not modify
`search_aftersale_policy`, retrieval algorithms, evidence merge behavior, RAG evaluation runner, policy ingestion, or
evidence-only semantics.

## Provider Metrics Boundary

Provider metrics are a foundation hook. V5.B.3.2 does not call real LLMs, does not call real embedding providers,
does not call Spring AI live providers, and does not call Spring AI `VectorStore` by default.

## Actuator Exposure Boundary

Actuator web exposure remains health-only. `/actuator/metrics`, `/actuator/prometheus`, `/actuator/env`,
`/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`, and `/actuator/threaddump` remain unavailable by
default.

## Prometheus / OpenTelemetry Boundary

Prometheus registry, `/actuator/prometheus`, Grafana dashboards, OpenTelemetry SDK/exporter/collector configuration,
distributed tracing, cross-service trace-id propagation, and production monitoring backend remain future / opt-in work.

## Secret Safety Boundary

Metric tags and docs must not expose API keys, passwords, tokens, private endpoints, JDBC URLs, raw prompts, raw
provider responses, search queries, snippets, raw dataset paths, local absolute paths, ticket IDs, order IDs, user IDs,
or AgentRun IDs.

## Runtime Non-change Boundary

No business feature was added. The task does not modify `search_aftersale_policy` runtime, retrieval algorithm, RAG
evaluation runner, Actuator health behavior, OpenAPI runtime behavior, ToolRegistry execution semantics, ToolCallTrace
schema, Workspace evidence logic, Execution Tree runtime, migration SQL, Docker / CI, or external business
integrations.

## Default Offline Boundary

Default validation remains offline and deterministic. It does not require real LLM, API Key, PostgreSQL, PGvector,
Docker, MySQL, Redis, real embedding provider, Spring AI live calls, Spring AI `VectorStore`, Prometheus,
OpenTelemetry collector, Docker Compose, or external network.

## Validation Commands

```bash
mvn test -Dtest=ApplicationMetricsRecorderTest
mvn test -Dtest=MetricsFoundationBoundaryTest
mvn test -Dtest=MetricsFoundationDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- `/actuator/metrics` and `/actuator/prometheus` are not exposed by default.
- Prometheus registry and dashboards are not implemented.
- OpenTelemetry tracing and cross-service propagation are not implemented.
- Provider cost metrics and production monitoring are not implemented.
- Production auth, Kubernetes / Helm, release / rollback hardening, and real external business integrations remain
  future work.

## Follow-ups

- V5.B.3.3 tracing / cross-service propagation strategy.
- V5.B.3.4 tracing / correlation boundary completed for local HTTP log correlation.
- V5.B.3.5 planned production monitoring roadmap.
- V5.B.4 auth, Kubernetes / Helm, release and rollback hardening.

## Completion Signal

TASK_COMPLETE
