# V5.B.3.1 Readiness / Liveness Boundary

Date: 2026-06-03

Status: Completed

## Goal

Add the minimal Spring Boot Actuator readiness / liveness probe boundary while preserving the existing default
health-only exposure and offline validation guarantees.

## Scope Completed

- Enabled Spring Boot health probes in `application.yml`.
- Added explicit `liveness` and `readiness` health groups.
- Kept Actuator web exposure limited to `health`.
- Added runtime boundary tests for health, liveness, readiness, sensitive endpoint exposure, and default offline
  dependency beans.
- Added docs harness coverage for status, safety, boundaries, and validation commands.
- Added deployment documentation for readiness / liveness probe semantics.
- Updated README, validation, quality, deployment, production config, remediation, and active correction status docs.

## What Changed

- `/actuator/health/liveness` is available through the health endpoint.
- `/actuator/health/readiness` is available through the health endpoint.
- Sensitive actuator endpoints remain unavailable by default.
- Default context still avoids `DataSource`, Spring AI live model, Spring AI `VectorStore`, and
  `JdbcPolicyVectorRepository` beans.

## Liveness Boundary

Liveness is the application process and lifecycle-state probe. It does not call live providers, databases, vector
stores, Docker, Redis, or external network services.

## Readiness Boundary

Readiness is a basic traffic-readiness signal for the default offline / local profile. It is not live DB, PGvector,
LLM, embedding provider, Spring AI `VectorStore`, production auth, or production monitoring validation.

## Actuator Exposure Boundary

Actuator web exposure remains health-only. The default exposed paths are:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

The following remain unavailable by default:

- `/actuator/env`
- `/actuator/beans`
- `/actuator/configprops`
- `/actuator/heapdump`
- `/actuator/threaddump`
- `/actuator/prometheus`

## Secret Safety Boundary

Health responses must not expose API keys, database passwords, tokens, private endpoints, JDBC URLs, raw prompts, raw
provider responses, local absolute paths, or raw dataset paths. Health details remain hidden by default.

## Live Dependency Boundary

V5.B.3.1 does not add live readiness checks for PostgreSQL, PGvector, MySQL, Redis, real LLMs, real embedding
providers, Spring AI live providers, Spring AI `VectorStore`, Docker, or external network services.

## Metrics / Tracing Boundary

V5.B.3.1 does not add Micrometer business metrics, Prometheus registry, `/actuator/prometheus`, Grafana dashboards,
OpenTelemetry, collector configuration, distributed tracing, or production log aggregation. These remain future /
opt-in production hardening work.

## Runtime Non-change Boundary

No business runtime behavior was changed. This phase does not modify ToolRegistry, `search_aftersale_policy`, RAG
retrieval, ingestion, health indicator internals, OpenAPI config, ToolCallTrace, Workspace, Execution Tree, Ticket,
AgentRun, Approval, or production application services.

## Default Offline Boundary

Default validation remains offline and deterministic. It does not require real LLM, API Key, PostgreSQL, PGvector,
Docker, MySQL, Redis, real embedding provider, Spring AI live provider calls, secret manager, Docker Compose,
Prometheus, OpenTelemetry collector, or external network.

## Validation Commands

```bash
mvn test -Dtest=ReadinessLivenessBoundaryTest
mvn test -Dtest=ReadinessLivenessBoundaryDocsTest
mvn test
mvn checkstyle:check
mvn spotbugs:check
mvn test -Dtest=ArchitectureTest
```

## Known Limitations

- This is readiness / liveness probe boundary work only.
- Production monitoring is not completed.
- Prometheus and OpenTelemetry integrations are not completed.
- Live dependency readiness checks are not completed.
- Production auth, Kubernetes / Helm, release automation, rollback strategy, and external business integrations remain
  future work.

## Follow-ups

- V5.B.3.2: Micrometer metrics and optional registry strategy.
- V5.B.3.3: tracing and cross-service propagation strategy.
- V5.B.3.4 tracing / correlation boundary completed for local HTTP log correlation.
- V5.B.3.5: planned production monitoring roadmap.
- V5.B.4: auth, Kubernetes / Helm, release and rollback hardening.

## Completion Signal

TASK_COMPLETE
