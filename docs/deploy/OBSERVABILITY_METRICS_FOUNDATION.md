# V5.B.3.2 Micrometer Metrics Foundation

Date: 2026-06-03

Status: Completed for low-cardinality Micrometer application metrics foundation.

## Goal

V5.B.3.2 adds a project-owned Micrometer metrics recording boundary for core application events while keeping Actuator
web exposure limited to health. The goal is to make AgentRun, ToolCall, Approval, RAG search, and provider-call
observations measurable through `MeterRegistry` without introducing a production monitoring backend.

This is not Prometheus integration, not OpenTelemetry tracing, not Grafana dashboard work, and not production
monitoring.

## Metric Names

Project-owned meters use the `aftersale.*` prefix:

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

## Low-cardinality Tags

Allowed tags are intentionally bounded:

- `component`
- `operation`
- `outcome`
- `status`
- `tool_name`
- `risk_level`
- `retrieval_mode`
- `fallback`
- `provider_type`
- `approval_decision`

Metric tags must not contain user identifiers, ticket identifiers, order identifiers, AgentRun identifiers, raw
prompts, raw provider responses, policy snippets, search queries, API keys, database credentials, tokens, local
absolute paths, JDBC URLs, raw dataset paths, or private endpoints.

Unsafe, blank, too-long, path-like, URL-like, credential-like, prompt-like, query-like, or free-text-ish tag values are
sanitized to `unknown`.

## Instrumented Boundaries

AgentRun metrics record started and completed outcomes with duration. They do not change AgentRun state transitions,
planning, ToolRegistry execution, ToolCallTrace writes, Workspace writes, or Execution Tree runtime.

ToolCall metrics are recorded around `ToolRegistry` execution outcomes. Tool execution still goes through
`ToolRegistry`; metrics do not bypass RiskPolicy, Approval, or ToolCallTrace.

Approval metrics record approval request and decision counts. Approval state behavior is unchanged.

RAG search metrics record retrieval mode, fallback flag, outcome, and duration. They do not modify
`search_aftersale_policy`, retrieval algorithms, evidence merge behavior, RAG evaluation, or policy evidence semantics.
RAG evidence remains policy evidence only, not a business decision or business action.

Provider metrics provide a foundation hook for provider call counters and duration. V5.B.3.2 does not call real LLMs,
does not call real embedding providers, does not call Spring AI live providers, and does not call Spring AI
`VectorStore` by default.

## Actuator Exposure Boundary

Actuator web exposure remains health-only. The following paths remain unavailable by default:

- `/actuator/metrics`
- `/actuator/prometheus`
- `/actuator/env`
- `/actuator/beans`
- `/actuator/configprops`
- `/actuator/heapdump`
- `/actuator/threaddump`

The metrics foundation records meters in the application `MeterRegistry`, but it does not expose a metrics HTTP
endpoint by default.

## Prometheus / OpenTelemetry Boundary

V5.B.3.2 does not add:

- Prometheus registry;
- `/actuator/prometheus`;
- Grafana dashboards;
- OpenTelemetry SDK, exporter, or collector configuration;
- distributed tracing;
- cross-service trace-id propagation;
- production monitoring backend;
- provider cost metrics dashboard.

Those remain V5.B.3.3 / V5.B.3.4 or later opt-in production hardening work.

## Default Offline Validation

Targeted validation:

```bash
mvn test -Dtest=ApplicationMetricsRecorderTest
mvn test -Dtest=MetricsFoundationBoundaryTest
mvn test -Dtest=MetricsFoundationDocsTest
```

Default gate:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

The default gate does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Spring AI `VectorStore`, Prometheus, OpenTelemetry collector, secret manager,
Docker Compose, or external network.

## Follow-ups

- V5.B.3.3: tracing / cross-service propagation strategy.
- V5.B.3.4: production monitoring and dashboard roadmap.
- V5.B.4: auth, Kubernetes / Helm, release and rollback hardening.
