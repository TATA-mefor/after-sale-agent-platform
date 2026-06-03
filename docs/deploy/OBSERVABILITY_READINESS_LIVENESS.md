# V5.B.3.1 Readiness / Liveness Boundary

Date: 2026-06-03

Status: Completed for readiness / liveness actuator probe boundary.

## Goal

V5.B.3.1 adds the minimal Spring Boot Actuator readiness / liveness boundary for the default application profile. It
keeps the existing health-only exposure model and documents what the probes mean in this repository.

This is not production monitoring, not a Prometheus integration, not OpenTelemetry tracing, and not a live dependency
connectivity check.

## Exposed Health Paths

The default Actuator web exposure remains limited to `health`.

Available health paths:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

Endpoints that remain unavailable by default:

- `/actuator/env`
- `/actuator/beans`
- `/actuator/configprops`
- `/actuator/heapdump`
- `/actuator/threaddump`
- `/actuator/prometheus`

## Liveness Boundary

Liveness means the Spring Boot process and application lifecycle state can answer the health probe. It is intentionally
small and should not call external systems.

Default liveness does not connect to:

- real LLM providers;
- real embedding providers;
- PostgreSQL;
- PGvector;
- MySQL;
- Redis;
- Docker;
- Spring AI live provider APIs;
- Spring AI `VectorStore`;
- external network services.

## Readiness Boundary

Readiness means the application is basically ready to receive traffic under the current default profile. In this
project, the default profile is offline / local and uses fake or in-memory dependencies.

Default readiness does not prove live PostgreSQL / PGvector connectivity, real embedding quality, live LLM provider
reachability, vector index freshness, production auth, production monitoring, or deployment readiness.

Live dependency readiness checks remain future / opt-in work. They must be added only with explicit profile or system
property gates so default `mvn test` stays offline and deterministic.

## Secret Safety Boundary

Health probe responses must not expose:

- API keys;
- database passwords;
- tokens;
- private endpoints;
- JDBC URLs;
- raw prompts;
- raw provider responses;
- local absolute paths;
- raw dataset paths.

Health details remain hidden by default with `management.endpoint.health.show-details=never`.

## Metrics / Tracing Boundary

V5.B.3.1 does not add Micrometer business metrics, Prometheus registry, Grafana dashboards, OpenTelemetry exporters,
collector configuration, distributed tracing, or production log aggregation.

Those items remain V5.B.3.2+ or later production hardening work.

## Default Offline Validation

Targeted validation:

```bash
mvn test -Dtest=ReadinessLivenessBoundaryTest
mvn test -Dtest=ReadinessLivenessBoundaryDocsTest
```

Default gate:

```bash
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

The default gate does not require real LLM, API Key, PostgreSQL, PGvector, Docker, MySQL, Redis, real embedding
provider, Spring AI live provider calls, secret manager, Docker Compose, Prometheus, OpenTelemetry collector, or
external network.

## Follow-ups

- V5.B.3.2: low-cardinality Micrometer metrics strategy and optional registry decision.
- V5.B.3.3: tracing / cross-service propagation decision.
- V5.B.3.4: production monitoring and dashboard roadmap.
- V5.B.4: auth, Kubernetes / Helm, release and rollback hardening.
