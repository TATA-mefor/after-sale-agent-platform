# V5.B.3.5 Observability Docs + Completion Record

Date: 2026-06-04

Status: Completed

## Goal

V5.B.3.5 closes the current V5.B.3 observability documentation thread. It gives reviewers one concise map for the
readiness / liveness probe boundary, Micrometer metrics foundation, Prometheus opt-in exposure, local HTTP correlation
boundary, validation commands, and remaining production monitoring work.

This is a documentation and completion-record task. It does not add runtime observability behavior.

## Current Observability Baseline

- V5.B.3.1 completed readiness / liveness Actuator probe documentation and tests.
- V5.B.3.2 completed low-cardinality Micrometer metrics foundation documentation and tests.
- V5.B.3.3 completed explicit `observability-prometheus` profile documentation and tests.
- V5.B.3.4 completed local HTTP `X-Correlation-Id` / `X-Request-Id` correlation documentation and tests.
- V5.B.3.5 completes the observability docs index, production monitoring roadmap wording, and completion record.

## Documentation Map

- `docs/deploy/OBSERVABILITY_READINESS_LIVENESS.md`
- `docs/deploy/OBSERVABILITY_METRICS_FOUNDATION.md`
- `docs/deploy/OBSERVABILITY_PROMETHEUS_OPT_IN.md`
- `docs/deploy/OBSERVABILITY_TRACING_CORRELATION.md`
- `docs/deploy/OBSERVABILITY_DOCS_COMPLETION.md`
- `docs/exec-plans/completed/EXEC_PLAN_V5_B3_5_OBSERVABILITY_DOCS_COMPLETION_RECORD.md`
- `docs/quality/VALIDATION_COMMANDS.md`
- `docs/quality/QUALITY_SCORE.md`
- `docs/deploy/DEPLOYMENT_HARDENING_ROADMAP.md`

## Production Monitoring Roadmap Boundary

V5.B.3.5 documents the production monitoring roadmap, but it does not implement a production monitoring backend.

Future / opt-in work remains:

- Grafana dashboards;
- Prometheus scrape jobs, alert rules, and recording rules;
- external log aggregation;
- OpenTelemetry SDK, exporters, collector, tracing backend, and sampling policy;
- cross-service propagation such as W3C `traceparent`;
- provider cost dashboards;
- production incident response runbooks.

## Metrics Boundary

Metrics remain low-cardinality. Correlation IDs, request IDs, raw prompts, raw user messages, policy snippets, local
paths, URLs, JDBC URLs, API keys, passwords, tokens, and secrets must not be used as metric tags.

`/actuator/metrics` is not exposed by default. `/actuator/prometheus` is exposed only with the explicit
`observability-prometheus` profile.

## Tracing Boundary

Tracing remains local HTTP log correlation only. V5.B.3.5 does not add OpenTelemetry, distributed tracing,
cross-service trace propagation, Jaeger, Zipkin, collector configuration, tracing backend, or production tracing.

## Logging Boundary

Structured logs include local request/correlation fields for diagnostics. Logs are not the audit source of truth.
ToolCallTrace, Approval records, Workspace summaries, and Execution Tree remain the project audit and explanation
surfaces.

## Runtime Non-change Boundary

V5.B.3.5 does not change:

- AgentRun state transitions;
- ToolRegistry execution semantics;
- `search_aftersale_policy` runtime;
- RAG retrieval algorithm;
- ToolCallTrace schema or write behavior;
- Workspace evidence logic;
- Execution Tree runtime;
- Actuator health indicator behavior;
- OpenAPI runtime behavior;
- Flyway, Docker, CI, or profile behavior.

## Default Offline Boundary

Default validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Prometheus server, Grafana, OpenTelemetry collector, log aggregation backend,
tracing backend, or external network.

## Validation Commands

```bash
mvn test -Dtest=ObservabilityDocsCompletionDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- Production monitoring is not completed.
- Production tracing is not completed.
- Production dashboards and alerting are not completed.
- Production log aggregation is not completed.
- Production auth, Kubernetes / Helm, and release / rollback hardening remain future work.

## Follow-ups

- V5.B.4: production auth / Kubernetes / Helm / release and rollback hardening.
  V5.B.4.3 K8s / Helm Foundation completed (see `docs/deploy/K8S_HELM_FOUNDATION.md`).
  V5.B.4.4 Release / Rollback Foundation completed (see `docs/deploy/RELEASE_ROLLBACK_FOUNDATION.md`).
  Release review confirms readiness/liveness and correlation logging; Prometheus remains
  opt-in; production monitoring backend remains future.
- Future observability hardening: production monitoring backend, dashboards, alerts, log aggregation, and
  OpenTelemetry evaluation.

## Completion Signal

TASK_COMPLETE
