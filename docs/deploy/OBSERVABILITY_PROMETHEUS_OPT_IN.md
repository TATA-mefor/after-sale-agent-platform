# V5.B.3.3 Prometheus Opt-in Exposure

Date: 2026-06-04

Status: Completed for Prometheus opt-in exposure.

## Goal

V5.B.3.3 adds a Prometheus registry dependency and an explicit `observability-prometheus` profile so reviewers can
inspect Micrometer meters through `/actuator/prometheus` only when they intentionally opt in.

This is an endpoint exposure boundary, not a production monitoring implementation.

## Opt-in Profile

Prometheus exposure is enabled only with:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=observability-prometheus
```

The profile uses:

```yaml
management:
  endpoint:
    prometheus:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,prometheus
```

`AFTERSALE_PROMETHEUS_ENABLED=false` can disable the Prometheus endpoint even when the profile is active.

## Default Exposure Boundary

The default profile remains health-only:

- `/actuator/health` is available.
- `/actuator/health/liveness` is available.
- `/actuator/health/readiness` is available.
- `/actuator/prometheus` is unavailable by default.
- `/actuator/metrics` is unavailable by default.
- `/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`, and `/actuator/threaddump` remain
  unavailable.

## Metrics Policy Boundary

The Micrometer recorder remains application-owned and low-cardinality. Prometheus is only a scrape format and endpoint
for existing meters. It does not change AgentRun state transitions, ToolRegistry execution, Approval behavior,
`search_aftersale_policy`, RAG retrieval, ToolCallTrace writes, Workspace logic, or Execution Tree runtime.

Metric tags must not include API keys, passwords, tokens, local paths, raw prompts, raw provider responses, raw user
messages, raw policy snippets, JDBC URLs, or high-cardinality IDs.

## OpenTelemetry Boundary

V5.B.3.3 does not add OpenTelemetry SDK, exporters, collector configuration, distributed tracing, cross-service
trace-id propagation, or span design. Those remain future / planned work.

## Production Monitoring Boundary

V5.B.3.3 does not add Grafana dashboards, alert rules, scrape jobs, recording rules, production log aggregation,
provider cost dashboards, incident response, SLOs, production auth, Kubernetes / Helm, release automation, or deployment
hardening.

The opt-in endpoint is useful for local review and future production hardening design, but it is not a production
monitoring claim.

## Default Offline Boundary

Default validation does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, Spring AI `VectorStore`, Docker Compose, Prometheus server, Grafana,
OpenTelemetry collector, or external network.

The `observability-prometheus` profile exposes a local Actuator scrape endpoint only. It does not connect to a
Prometheus server and does not call live providers.

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

- `/actuator/metrics` remains intentionally unexposed.
- No Prometheus scrape configuration is provided.
- No Grafana dashboard is provided.
- No OpenTelemetry tracing or cross-service propagation is implemented.
- No production monitoring backend is completed.

## Follow-ups

- V5.B.3.4 planned production monitoring roadmap.
- Future OpenTelemetry / tracing strategy if the deployment topology requires cross-service trace propagation.
- V5.B.4 planned auth, Kubernetes / Helm, release and rollback hardening.
