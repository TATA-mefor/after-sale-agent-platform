# V5.B.3.3 Prometheus Opt-in Exposure

Date: 2026-06-04

Status: Completed

## Goal

Add Prometheus as an explicit opt-in Actuator exposure path while preserving the default health-only, offline,
deterministic validation boundary.

## Scope Completed

- Added Spring Boot managed `micrometer-registry-prometheus`.
- Kept default Actuator web exposure limited to `health`.
- Added `observability-prometheus` profile for `/actuator/prometheus`.
- Kept `/actuator/metrics` unavailable.
- Added runtime and docs harness coverage.
- Updated V5.B.3 status docs.

## What Changed

The project now has a local, explicit Prometheus scrape endpoint for review when the profile is enabled. The default
profile remains unchanged for normal validation: health probes are exposed, Prometheus and sensitive Actuator endpoints
are not exposed.

## Prometheus Registry Boundary

Prometheus registry support is present as a Micrometer registry dependency. This does not add a Prometheus server,
scrape job, dashboard, alert rule, production monitoring backend, or provider cost dashboard.

## Opt-in Profile Boundary

`observability-prometheus` is the only project profile that exposes `/actuator/prometheus`. The profile exposes
`health,prometheus` only. `AFTERSALE_PROMETHEUS_ENABLED=false` can disable the endpoint while the profile is active.

## Default Exposure Boundary

Default `application.yml` keeps:

```yaml
management.endpoints.web.exposure.include: health
management.endpoint.prometheus.enabled: false
```

`/actuator/prometheus`, `/actuator/metrics`, `/actuator/env`, `/actuator/beans`, `/actuator/configprops`,
`/actuator/heapdump`, and `/actuator/threaddump` remain unavailable by default.

## Actuator Endpoint Boundary

The enabled opt-in endpoint is a read-only metrics scrape endpoint. It does not create admin endpoints and does not
broaden sensitive Actuator exposure.

## Metrics Policy Boundary

Project meters remain low-cardinality and application-owned. Prometheus exposure does not alter AgentRun, ToolRegistry,
Approval, RAG retrieval, ToolCallTrace, Workspace, Execution Tree, health indicator, OpenAPI, or ingestion behavior.

## Secret Safety Boundary

Prometheus output must not expose API keys, passwords, tokens, JDBC URLs, local absolute paths, raw prompts, raw provider
responses, raw user messages, or raw dataset paths. The tests check representative secret and path markers.

## OpenTelemetry Boundary

V5.B.3.3 does not implement OpenTelemetry, distributed tracing, collector configuration, exporter configuration, or
cross-service trace-id propagation. Those remain future / planned work.

## Production Monitoring Boundary

V5.B.3.3 is not production monitoring. It does not add Grafana dashboards, alerting, scrape configuration, SLOs,
production incident response, production auth, Kubernetes / Helm, release automation, or deployment hardening.

## Runtime Non-change Boundary

No business runtime behavior changed. The task does not modify `search_aftersale_policy`, retrieval algorithms, RAG
evaluation, Actuator health indicator logic, OpenAPI behavior, ToolRegistry semantics, ToolCallTrace schema, Workspace
evidence logic, Execution Tree runtime, AgentApplicationService, or external business integrations.

## Default Offline Boundary

Default validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Spring AI `VectorStore`, Docker Compose, Prometheus server, Grafana,
OpenTelemetry collector, or external network.

## Validation Commands

```bash
mvn test -Dtest=PrometheusOptInExposureTest
mvn test -Dtest=PrometheusOptInDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- `/actuator/metrics` remains intentionally unavailable.
- No production scrape configuration is included.
- No Grafana dashboards or alerts are included.
- OpenTelemetry tracing remains future work.
- Production monitoring remains future work.

## Follow-ups

- V5.B.3.4 tracing / correlation boundary completed for local HTTP log correlation.
- V5.B.3.5 observability docs + completion record completed; production monitoring implementation remains future /
  opt-in.
- Future OpenTelemetry / cross-service propagation strategy.
- V5.B.4 auth, Kubernetes / Helm, release and rollback hardening.

## Completion Signal

TASK_COMPLETE
