# V5.B.3.4 Tracing / Correlation Boundary

Date: 2026-06-04

Status: Completed for local HTTP tracing / correlation boundary.

## Goal

V5.B.3.4 adds a local HTTP correlation boundary for reviewers and maintainers to follow one request through structured
logs without adding distributed tracing infrastructure.

The boundary covers `X-Correlation-Id`, `X-Request-Id`, response headers, MDC keys, structured logging, and secret-safe
header handling. It is not OpenTelemetry, not distributed tracing, not cross-service propagation, not production
tracing, and not production monitoring.

## HTTP Header Boundary

Supported request and response headers:

- `X-Correlation-Id`
- `X-Request-Id`

For each request, the application reads both headers independently. If a header is missing or unsafe, the application
generates a local replacement value. The response always returns sanitized safe values for both headers.

Unsafe header values are never echoed back and are never placed into MDC.

## Safe Value Boundary

Correlation and request identifiers are bounded to safe characters and length:

- safe characters: `[A-Za-z0-9._:-]`
- maximum length: `128`

The application rejects blank values, values with whitespace or control characters, too-long values, URL-like values,
path-like values, and credential-like values. Unsafe values are replaced with generated identifiers.

The filter must not log raw invalid header values. It must not put secrets, local paths, URLs, raw prompts, raw provider
responses, raw user text, ticket IDs, order IDs, AgentRun IDs, or user IDs into these correlation MDC fields.

## MDC Boundary

The filter writes only these MDC keys for this feature:

- `correlationId`
- `requestId`

Both keys are cleared in a `finally` block after request processing. They are diagnostic fields for log correlation
only. They are not audit records, authorization identifiers, business identifiers, or cross-service trace context.

Existing business MDC fields remain separate from this feature.

## Structured Logging Boundary

The default logging pattern includes both:

- `correlationId=%X{correlationId:-}`
- `requestId=%X{requestId:-}`

Logs remain diagnostic. ToolCallTrace, ApprovalRequest, Workspace summaries, and Execution Tree remain the project
audit / explanation surfaces.

## Actuator Boundary

The default Actuator exposure remains health-only:

- `/actuator/health` is available.
- `/actuator/health/liveness` is available.
- `/actuator/health/readiness` is available.
- `/actuator/prometheus` is unavailable by default.
- `/actuator/metrics` is unavailable by default.
- `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`, and `/actuator/threaddump` remain
  unavailable by default.

V5.B.3.4 does not broaden Actuator exposure.

## Metrics Tag Boundary

Correlation and request identifiers must not be used as Micrometer tags. They are high-cardinality per-request values
and must stay in logs only.

The existing metrics policy remains low-cardinality. Metric tags must not include `correlationId`, `requestId`, user
identifiers, ticket identifiers, order identifiers, AgentRun identifiers, API keys, passwords, tokens, local paths,
raw prompts, raw provider responses, raw user text, or raw policy snippets.

## Agent / Tool / Trace Boundary

V5.B.3.4 does not change Agent runtime semantics:

- ToolRegistry remains the Agent tool execution entry.
- `search_aftersale_policy` remains a LOW-risk read-only ToolRegistry tool.
- RAG evidence remains policy evidence only, not a business action or business decision.
- ToolCallTrace schema and write behavior are unchanged.
- Workspace evidence logic is unchanged.
- Execution Tree runtime is unchanged.
- Approval behavior is unchanged.
- AgentRun state transitions are unchanged.

The correlation filter does not create Ticket, AgentRun, ToolCallTrace, Workspace, Approval, or Execution Tree records.

## OpenTelemetry Boundary

V5.B.3.4 is not OpenTelemetry. It does not add:

- OpenTelemetry SDK;
- span creation;
- exporter configuration;
- collector configuration;
- W3C `traceparent`;
- distributed tracing;
- cross-service propagation;
- Jaeger or Zipkin integration.

Those remain future / opt-in production hardening work if the deployment topology requires them.

## Production Tracing Boundary

V5.B.3.4 is not production tracing and not production monitoring. It does not add:

- Grafana dashboards;
- scrape jobs;
- alerts;
- log aggregation backend;
- tracing backend;
- incident response;
- SLOs;
- production auth;
- Kubernetes / Helm;
- release / rollback automation.

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
- No distributed trace context is propagated.
- No OpenTelemetry span model is implemented.
- No production tracing backend is configured.
- No production monitoring dashboard is provided.

## Follow-ups

- V5.B.3.5 planned production monitoring roadmap.
- Future OpenTelemetry / distributed tracing strategy if cross-service topology requires it.
- V5.B.4 planned auth, Kubernetes / Helm, release and rollback hardening.
